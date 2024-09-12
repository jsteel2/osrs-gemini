import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AIHistory {
    public ArrayList<Map<String, Object>> contents = new ArrayList<>();
    public ArrayList<Map<String, Integer>> safetySettings = new ArrayList<>();
    private String systemPrompt = "You are playing the game Old School RunesScape, you will be sent a screenshot of the game and extra information about the world around you.\nInteractable objects will have numeric ids on them that you can use.\nExamine the screenshots carefully and read all the text, for example if the game tells you to talk to someone, talk to them, TALK TO THE SURVIVAL EXPERT. think carefully about what to do and output your thoughts before doing anything else.\nThen respond with a command by encasing it in backquotes at the end of your message, you have the following commands available:\ninteract, id, action # Interacts with an id\ncontinueDialog # Continues the dialog MUST ONLY BE USED WHILE IN DIALOG\ndialog, id # Chooses a dialog option\nopenTab, tab # use this if you are told to click on a tab (like the settings menu for example), tab must be any of accountManagement, clan, combat, emotes, equipment, friends, inventory, logout, magic, music, options, prayer, quest or skills\nHere's an example of calling a function: `interact, 2, Talk-to`\nYou MUST ALWAYS call one and only one function at the end of your response.\nIf you cant progress and keep doing the same thing, try to interact with different objects of the same kind.\nThat's it for now, do you understand?";

    public AIHistory() {
        Map<String, Integer> m = new HashMap<>();
        m.put("category", 10);
        m.put("threshold", 4);
        safetySettings.add(m);
        m = new HashMap<>();
        m.put("category", 8);
        m.put("threshold", 4);
        safetySettings.add(m);
        m = new HashMap<>();
        m.put("category", 7);
        m.put("threshold", 4);
        safetySettings.add(m);
        m = new HashMap<>();
        m.put("category", 9);
        m.put("threshold", 4);
        safetySettings.add(m);
        add("user", systemPrompt, null);
        add("model", "I have understood the instructions and will do as such.", null);
    }

    public void add(String role, String message, String file) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", role);
        ArrayList<Map<String, Object>> arr = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        m.put("text", message);
        arr.add(m);
        if (file != null) {
            Map<String, Object> d = new HashMap<>();
            Map<String, Object> d2 = new HashMap<>();
            d.put("mime_type", "image/png");
            d.put("file_uri", file);
            d2.put("file_data", d);
            arr.add(d2);
        }
        map.put("parts", arr);
        contents.add(map);
    }

    public void remove(int index) {
        contents.remove(index);
    }

    public String format() {
        StringBuilder b = new StringBuilder();
        for (Map<String, Object> m : contents) {
            ArrayList<Map<String, String>> m2 = (ArrayList<Map<String, String>>) m.get("parts");
            b.append(String.format("%s: %s\n", m.get("role"), m2.get(0).get("text")));
        }
        return b.toString();
    }
}
