import org.dreambot.api.Client;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widget;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.wrappers.cache.nodes.NodeWrapper;
import org.dreambot.api.wrappers.interactive.Entity;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.wrappers.interactive.interact.Interactable;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.WidgetChild;

import javax.swing.*;
import java.util.List;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

@ScriptManifest(name = "Gemini", description = "Gemini plays the game", author = "Joe Steele", version = 1.0, category = Category.MISC, image = "")
public class Gemini extends AbstractScript
{
    private Thread thread;
    private boolean stopThread;
    private ReentrantLock lock = new ReentrantLock();
    private String prompt;
    private JTextArea te;
    private JScrollPane scroll;
    private ArrayList<Object> interactables = new ArrayList<>();

    @Override
    public int onLoop() {
        return 999999999;
    }

    @Override
    public void onStart() {
        stopThread = false;
        thread = new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("GeminiAI");
                te = new JTextArea("Loading");
                te.setEditable(false);
                te.setLineWrap(true);
                scroll = new JScrollPane(te);
                scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

                frame.getContentPane().add(scroll);
                frame.setLocationRelativeTo(null);
                frame.pack();
                frame.setVisible(true);
            });
            sleep(10000);
            HTTPAI ai = new HTTPAI();
            while (true) {
                if (stopThread) return;
                lock.lock();
                BufferedImage image = Client.getCanvasImage();
                String p = prompt;
                String[] cmd = ai.prompt(p, image);
                SwingUtilities.invokeLater(() -> {
                    te.setText(ai.format());
                    JScrollBar vertical = scroll.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });
                switch (cmd[0]) {
                    case "interact":
                        interact(interactables.get(Integer.parseInt(cmd[1])), cmd[2]);
                        break;
                    case "continueDialog":
                        List<WidgetChild> w = Widgets.getAllContainingText("Click here to continue");
                        if (w.isEmpty()) break;
                        w.get(0).interact();
                        break;
                    case "dialog":
                        interact(interactables.get(Integer.parseInt(cmd[1])), null);
                        break;
                    case "openTab":
                        switch (cmd[1]) {
                            case "accountManagement": Tabs.open(Tab.ACCOUNT_MANAGEMENT); break;
                            case "clan": Tabs.open(Tab.CLAN); break;
                            case "combat": Tabs.open(Tab.COMBAT); break;
                            case "emotes": Tabs.open(Tab.EMOTES); break;
                            case "equipment": Tabs.open(Tab.EQUIPMENT); break;
                            case "friends": Tabs.open(Tab.FRIENDS); break;
                            case "inventory": Tabs.open(Tab.INVENTORY); break;
                            case "logout": Tabs.open(Tab.LOGOUT); break;
                            case "magic": Tabs.open(Tab.MAGIC); break;
                            case "music": Tabs.open(Tab.MUSIC); break;
                            case "options": Tabs.open(Tab.OPTIONS); break;
                            case "prayer": Tabs.open(Tab.PRAYER); break;
                            case "quest": Tabs.open(Tab.QUEST); break;
                            case "skills": Tabs.open(Tab.SKILLS); break;
                        }
                        break;
                    case "run":
                        if (Walking.isRunEnabled()) {
                            Walking.toggleRun();
                            sleep(200);
                            Walking.toggleRun();
                        } else {
                            Walking.toggleRun();
                        }
                        break;
                    case "move":
                        Tile t = Players.getLocal().getTile();
                        switch (cmd[1]) {
                            case "north": Walking.walk(t.getX(), t.getY() + 10); break;
                            case "south": Walking.walk(t.getX(), t.getY() - 10); break;
                            case "east": Walking.walk(t.getX() + 10, t.getY()); break;
                            case "west": Walking.walk(t.getX() - 10, t.getY()); break;
                        }
                        break;
                }
                lock.unlock();
                sleep(5000);
            }
        });
        thread.start();
    }

    @Override
    public void onExit() {
        stopThread = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPaint(Graphics g) {
        if (!lock.tryLock()) {
            return;
        }
        StringBuilder b = new StringBuilder();
        interactables.clear();

        Player p = Players.getLocal();
        g.setColor(Color.WHITE);
        Rectangle a = p.getBoundingBox();
        g.drawRect(a.x, a.y, a.width, a.height);
        g.drawString("You", p.getCenterPoint().x, p.getCenterPoint().y);
        b.append("NPCs:\n");

        int i = 0;
        for (NPC npc : NPCs.all()) {
            if (true) {
                Object[] ax = Arrays.stream(npc.getActions()).filter(Objects::nonNull).toArray();
                if (ax.length == 0) continue;
                interactables.add(npc);
                Rectangle c = npc.getBoundingBox();
                if (npc.isOnScreen()) {
                    g.setColor(Color.getHSBColor(npc.getID() * 16777619 / 360f, 1, 1));
                    g.drawString(Integer.toString(i), npc.getCenterPoint().x, npc.getCenterPoint().y);
                }
                b.append(String.format("%d: \"%s\", Actions: %s\n", i, npc.getName(), Arrays.toString(ax)));
                i += 1;
            }
        }

        b.append("Objects:\n");
        for (GameObject obj: GameObjects.all()) {
            if (obj.canReach()) {
                Object[] ax = Arrays.stream(obj.getActions()).filter(Objects::nonNull).toArray();
                if (ax.length == 0 && !obj.getName().equals("Fire")) continue;
                interactables.add(obj);
                Rectangle c = obj.getBoundingBox();
                if (obj.isOnScreen()) {
                    g.setColor(Color.getHSBColor(obj.getID() * 16777619 / 360f, 1, 1));
                    g.drawString(Integer.toString(i), obj.getCenterPoint().x, obj.getCenterPoint().y);
                }
                b.append(String.format("%d: \"%s\", Actions: %s\n", i, obj.getName(), Arrays.toString(ax)));
                i += 1;
            }
        }

        b.append("Inventory:\n");
        for (Item item : Inventory.all()) {
            if (item == null) continue;
            interactables.add(item);
            Rectangle r = Inventory.slotBounds(item.getSlot());
            g.setColor(Color.getHSBColor(item.getID() * 16777619 / 360f, 1, 1));
            g.drawString(Integer.toString(i), r.x, r.y);
            Object[] ee = Arrays.stream(item.getActions()).filter(Objects::nonNull).toArray();
            List<Object> eee = Arrays.asList(ee);
            ArrayList<Object> eeee = new ArrayList<Object>();
            eeee.add("Use");
            for (Object o : eee) eeee.add(o);
            b.append(String.format("%d: \"%s\", Actions: %s\n", i, item.getName(), eeee));
            i += 1;
        }

        boolean diag = false;
        List<WidgetChild> widgets = Widgets.getAllContainingText("Click here to continue");
        if (!widgets.isEmpty()) {
            b.append("You are in a dialog, use the continueDialog command to continue talking or do something else.\n");
            diag = true;
        }

        WidgetChild ab = Widgets.get(219, 1);
        if (ab != null) {
            b.append("You are in a dialog, choose a dialog option with the dialog command or do something else.\nDialog options:\n");
            diag = true;
            int x = 1;
            WidgetChild[] s = ab.getChildren();
            WidgetChild y;
            while (x < s.length && (y = s[x]) != null && !y.getText().isEmpty()) {
                b.append(String.format("%d: \"%s\"", i, y.getText()));
                interactables.add(y);
                i += 1;
                x += 1;
            }
        }

        if (!diag) {
            b.append("You are currently NOT in a dialog, do not use continueDialog!\n");
        }

        prompt = b.toString();
        lock.unlock();
    }

    private void interact(Object x, String action) {
        int i = 5;
        if (x instanceof Item) {
            while (i-- > 0 && !((Item)x).interact(action)) sleep(200);
        } else if (x instanceof Interactable) {
            while (i-- > 0 && !((Interactable)x).interact(action)) {
                sleep(200);
                Walking.walk((Entity) x);
                sleep(1000);
            }
        } else if (x instanceof WidgetChild) {
            while (i-- > 0 && !((WidgetChild)x).interact()) sleep(200);
        }else {
            throw new RuntimeException("FUCK");
        }
    }
}