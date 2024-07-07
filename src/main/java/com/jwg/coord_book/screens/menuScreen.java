package com.jwg.coord_book.screens;

import com.jwg.coord_book.CoordBook;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.SelectionManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import java.io.*;
import java.util.*;
import java.util.List;

import static com.jwg.coord_book.CoordBook.*;

@Environment(EnvType.CLIENT)
public class menuScreen extends Screen {
    public static int page = 0;
    public static int bookmarkedpage = 0;
    public static int pageLimit = -1;
    public static final Identifier BOOK_TEXTURE = new Identifier("textures/gui/book.png");
    public static final Identifier BOOK_SIDEBAR_TEXTURE = new Identifier("coordbook:textures/gui/sidebar.png");
    private List<OrderedText> cachedPage;
    private Text pageIndexText;
    private final boolean pageTurnSound;
    public static boolean deletePageButtonShown = true;
    private String versionText;
    private String contents;
    private SelectionManager selectionManager;
    private int loc;

    public menuScreen() {
        this(true);
    }

    char ltchr;

    private menuScreen(boolean bl) {
        super(NarratorManager.EMPTY);
        this.contents = "";
        this.versionText = "";
        this.cachedPage = Collections.emptyList();
        this.pageIndexText = ScreenTexts.EMPTY;
        this.pageTurnSound = bl;
        this.loc = 0;
        this.ltchr = 0;
        pageLimit = pageLimit - 1;
    }

    protected void init() {
        assert this.client != null;
        this.client.keyboard.setRepeatEvents(true);
        this.addButtons();
        selectionMgr();
    }

    //To fix, currently just clears the page
    protected void removePage(int rmpage) {
        int files = Objects.requireNonNull(new File(pageLocation + "/").list()).length;
        int pagesToRename = files - rmpage;
        boolean tmp = false;
        if (rmpage == 0) { LOGGER.warn("Can't delete first page"); }
        else if (rmpage == files-1) { goToPreviousPage(); tmp = new File(pageLocation +"/"+ rmpage + ".jdat").delete(); }
        else {
            for (int i = 1; i < pagesToRename; i++) {
                int l = rmpage+i; int m = l-1;
                tmp = new File(pageLocation + "/" + l + ".jdat").renameTo(new File(pageLocation + "/" + m + ".jdat"));
            }
        }
        if (developerMode) { LOGGER.info(String.valueOf(tmp)); }
    }
    private void writeBookmark() {
        try {
            FileWriter fileOverwriter = new FileWriter("config/coordinate-book/bookmark.cfg");
            fileOverwriter.write("# This file stores the bookmark data; DO NOT EDIT THIS FILE as it can cause issues, bugs and crashing!\n"+bookmarkedpage);
            fileOverwriter.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
    protected void addButtons() {
        //Done button
        this.addDrawableChild(new ButtonWidget(this.width / 2 - 100, 196, 200, 20, ScreenTexts.DONE, (button) -> { assert this.client != null; this.client.setScreen(null); }));

        //Delete page button
        this.addDrawableChild(new TexturedButtonWidget(this.width/2 +86, 20, 17, 17, 0, 0, 17, DELETE_ICON, 17, 34, (button) -> removePage(page), Text.translatable("jwg.button.close")));
        //Bookmark button
        this.addDrawableChild(new TexturedButtonWidget(this.width/2 +86, 38, 17, 17, 0, 0, 17, BOOKMARK_ICON, 17, 34, (button) -> page = bookmarkedpage, Text.translatable("jwg.button.bookmark")));

        //Bookmark button
        this.addDrawableChild(new TexturedButtonWidget(this.width/2-60, 9, 20, 20, 0, 0, 20, BOOKMARK_MARKER_ICON, 32, 64, (icon) -> { bookmarkedpage = page; writeBookmark(); }, Text.translatable("jwg.button.bookmark-marker")));
        //Go to bookmark page button
        this.addDrawableChild(new TexturedButtonWidget(this.width/2 +86, 38, 17, 17, 0, 0, 17, BOOKMARK_ICON, 17, 34, (button) -> {
            if (page != bookmarkedpage && bookmarkedpage >= 0) {
                if (new File(pageLocation+"/"+bookmarkedpage+".jdat").exists()) { page = bookmarkedpage; assert this.client != null; this.client.setScreen(this);
                } else { bookmarkedpage = -1; }
            }
        }, Text.translatable("jwg.button.bookmark-marker")));


        //Page buttons (arrows)
        int i = (this.width - 192) / 2;
        this.addDrawableChild(new PageTurnWidget(i + 116, 159, true, (button) -> {
            if (page != pageLimit || pageLimit < 0) { if (page >= pageLimit && pageLimit > 0) { page = pageLimit; }this.goToNextPage(); assert this.client != null; this.client.setScreen(this); }
        }, this.pageTurnSound));
        this.addDrawableChild(new PageTurnWidget(i + 43, 159, false, (button) -> {
            this.goToPreviousPage();
            assert this.client != null;
            this.client.setScreen(this);
        }, this.pageTurnSound));

        if (developerMode) { this.versionText = "Coordinate Book "+version+" Developer build";
        } else { this.versionText = "Coordinate Book " + version; }
    }
    //I have no clue what this does but i think it can do what i want it to
    //It did not but if anyone wants to fix the rest of issue #22 be my guest
    //I will try fix this by 1.4.0.... maybe.... probably not but, hey, we can hope, right?
    protected void selectionMgr() {
        assert this.client != null;
        this.selectionManager = new SelectionManager(() -> this.contents, (text) -> this.contents = text, SelectionManager.makeClipboardGetter(this.client), SelectionManager.makeClipboardSetter(this.client), (text) -> this.client.textRenderer.getWidth(text) <= this.loc);
    }
    protected void goToPreviousPage() {
        --page;
        if (page <= -1) {
            page = 0;
        }
    }
    protected void goToNextPage() {
        ++page;
        if (!new File(pageLocation+"/"+page+".jdat").exists()) {
            try {
                if (new File(pageLocation+"/"+page+".jdat").createNewFile()) {
                    CoordBook.LOGGER.info("page {} has been created", page);
                }
            } catch (IOException e) {
                CoordBook.LOGGER.error("page {} is unable to be created", page);
                throw new RuntimeException(e);
            }
        }
    }
    public void renderBookText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_TEXTURE);
        int i = (this.width - 192) / 2;

        this.drawTexture(matrices, i, 2, 0, 0, 192, 192);
        this.pageIndexText = Text.translatable("book.pageIndicator", page + 1, Math.max((Objects.requireNonNull(new File(pageLocation+"/").list()).length), 1));

        drawStringWithShadow(matrices, this.textRenderer, String.valueOf(versionText), 2, this.height - 10, 16777215);

        StringBuilder fulldata = new StringBuilder();
        try {
            Scanner readPageContent = new Scanner(new File(pageLocation+"/"+page+".jdat"));
            while (readPageContent.hasNextLine()) {
                String data = readPageContent.nextLine();
                if (!fulldata.toString().equals("")) {
                    data = "\n" + data;
                }
                fulldata.append(data);
            }
            readPageContent.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.contents = String.valueOf(fulldata);
        StringVisitable stringVisitable = StringVisitable.plain(this.contents);
        this.cachedPage = this.textRenderer.wrapLines(stringVisitable, 114);

        int k = this.textRenderer.getWidth(this.pageIndexText);
        this.textRenderer.draw(matrices, this.pageIndexText, (float)(i - k + 192 - 44), 18.0F, 1);

        Objects.requireNonNull(this.textRenderer);
        int l = Math.min(128 / 9, this.cachedPage.size());

        for(int m = 0; m < l; ++m) {
            OrderedText orderedText = this.cachedPage.get(m);
            TextRenderer var10000 = this.textRenderer;
            float var10003 = (float)(i + 36);
            Objects.requireNonNull(this.textRenderer);
            var10000.draw(matrices, orderedText, var10003, (float)(32 + m * 9), 0);
        }
        super.render(matrices, mouseX, mouseY, delta);
    }
    public void renderSideBar(MatrixStack matrices, int mouseX, int mouseY, float delta){
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, BOOK_SIDEBAR_TEXTURE);
        int i = (this.width - -150) / 2;

        this.drawTexture(matrices, i, 2, 0, 0, 36, 180);
        super.render(matrices, mouseX, mouseY, delta);
    }
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        renderBookText(matrices, mouseX, mouseY, delta);
        renderSideBar(matrices, mouseX, mouseY, delta);
    }

    public boolean handleTextClick(Style style) {
        assert style != null;
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return false;
        } else if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
            String string = clickEvent.getValue();

            try {
                int i = Integer.parseInt(string) - 1;
                return this.goToPage(i);
            } catch (Exception var5) {
                return false;
            }
        } else {
            boolean bl = super.handleTextClick(style);
            if (bl && clickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                this.closeScreen();
            }

            return bl;
        }
    }
    private boolean goToPage(int i) {
        page = i;
        assert this.client != null;
        this.client.setScreen(this);
        return true;
    }
    public void closeScreen() {
        assert this.client != null;
        this.client.setScreen(null); }

    public static int countPg() throws IOException {
        File file = new File(pageLocation+"/"+page+".jdat");
        FileInputStream fileInputStream;

        { try { fileInputStream = new FileInputStream(file); } catch (FileNotFoundException e) { throw new RuntimeException(e); } }
        InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        int characterCount = 1;
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            characterCount += line.length();

        }
        return characterCount;
    }
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style style = this.getTextStyleAt(mouseX, mouseY);
            if (style != null && this.handleTextClick(style)) {
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    public boolean charTyped(char chr, int modifiers) {

        try { this.loc = countPg(); } catch (IOException e) { throw new RuntimeException(e); }
        this.ltchr = chr;
        this.selectionManager.insert(chr);
        String keystring = String.valueOf(this.ltchr);
        StringBuilder fulldata = new StringBuilder();
        this.selectionManager.putCursorAtEnd();
        this.selectionManager.insert(chr);
        try {
            Scanner readPageContent = new Scanner(new File(pageLocation+"/"+page+".jdat"));
            while (readPageContent.hasNextLine()) {
                String data = readPageContent.nextLine();
                if (!fulldata.toString().equals("")) {
                    data = "\n" + data;
                }
                fulldata.append(data);
            }
            readPageContent.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        fulldata = new StringBuilder(fulldata + keystring);
        this.contents = String.valueOf(fulldata);
        try {
            FileWriter updatePage = new FileWriter(pageLocation+"/"+page+".jdat");
            updatePage.write(String.valueOf(fulldata));
            updatePage.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(this.client).setScreen(this);
        return true;
    }


    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 259) {
            StringBuilder fulldata = new StringBuilder();
            try {
                Scanner readPageContent = new Scanner(new File(pageLocation+"/"+page+".jdat"));
                while (readPageContent.hasNextLine()) {
                    String data = readPageContent.nextLine();
                    if (!fulldata.toString().equals("")) {
                        data = "\n" + data;
                    }
                    fulldata.append(data);
                }
                readPageContent.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (!fulldata.toString().equals("")) {
                fulldata = new StringBuilder(fulldata.substring(0, this.contents.length() - 1));
                this.contents = String.valueOf(fulldata);
                try {
                    FileWriter updatePage = new FileWriter(pageLocation+"/"+page+".jdat");
                    updatePage.write(String.valueOf(fulldata));
                    updatePage.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (keyCode == 257) {
            StringBuilder fulldata = new StringBuilder();
            try {
                Scanner readPageContent = new Scanner(new File(pageLocation+"/"+page+".jdat"));
                while (readPageContent.hasNextLine()) {
                    String data = readPageContent.nextLine();
                    if (!fulldata.toString().equals("")) {
                        data = "\n" + data;
                    }
                    fulldata.append(data);
                }
                readPageContent.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (!fulldata.toString().equals("")) {
                fulldata = new StringBuilder(fulldata + "\n ");
                this.contents = String.valueOf(fulldata);
                try {
                    FileWriter updatePage = new FileWriter(pageLocation+"/"+page+".jdat");
                    updatePage.write(String.valueOf(fulldata));
                    updatePage.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
    @Nullable
    public Style getTextStyleAt(double x, double y) {
        if (!this.cachedPage.isEmpty()) {
            int i = MathHelper.floor(x - (double) ((this.width - 192) / 2) - 36.0);
            int j = MathHelper.floor(y - 2.0 - 30.0);
            if (i >= 0 && j >= 0) {
                Objects.requireNonNull(this.textRenderer);
                int k = Math.min(128 / 9, this.cachedPage.size());
                if (i <= 114) {
                    assert this.client != null;
                    Objects.requireNonNull(this.client.textRenderer);
                    if (j < 9 * k + k) {
                        Objects.requireNonNull(this.client.textRenderer);
                        int l = j / 9;
                        if (l < this.cachedPage.size()) {
                            OrderedText orderedText = this.cachedPage.get(l);
                            return this.client.textRenderer.getTextHandler().getStyleAt(orderedText, i);
                        }

                        return null;
                    }
                }

            }
        }
        return null;
    }

}