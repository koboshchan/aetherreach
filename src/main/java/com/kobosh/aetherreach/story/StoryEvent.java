package com.kobosh.aetherreach.story;

import java.util.List;
import java.util.ArrayList;

/**
 * One entry in story.json.
 *
 * type "dialog"  → title, text, texture, bgColor are used.
 * type "parkour" → length, jumps are used.
 *   jumps values: "forward2flat", "diag1flat", "diag1up", "forward1up"
 */
public class StoryEvent {
    public String type;

    // --- dialog fields ---
    public String title;
    public String text;
    public String texture;  // resource path e.g. "/char1.png"
    public String bgColor;  // hex e.g. "#1a2a3a"

    // --- parkour fields ---
    public int length;
    public List<String> jumps = new ArrayList<>();

    // --- die_dialog fields ---
    public List<StoryEvent> dialogs = new ArrayList<>();
}
