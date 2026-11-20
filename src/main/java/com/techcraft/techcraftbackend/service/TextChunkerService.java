package com.techcraft.techcraftbackend.service;

import java.util.List;

public interface TextChunkerService {

    List<String> chunk(String text);

    List<String> chunk(String text, int chunkSize, int overlap);
}
