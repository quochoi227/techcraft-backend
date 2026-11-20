package com.techcraft.techcraftbackend.service;

import com.techcraft.techcraftbackend.enums.ChatIntent;

public interface IntentClassifierService {
    ChatIntent classify(String message);
}
