package com.nextwatch.app;

import java.util.Arrays;
import java.util.List;

public class CatalogItem {
    public final String title;
    public final String type;
    public final int year;
    public final String commitment;
    public final String description;
    public final List<String> tags;

    public CatalogItem(String title, String type, int year, String commitment, String description, String... tags) {
        this.title = title;
        this.type = type;
        this.year = year;
        this.commitment = commitment;
        this.description = description;
        this.tags = Arrays.asList(tags);
    }
}
