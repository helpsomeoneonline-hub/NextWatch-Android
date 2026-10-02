package com.nextwatch.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Catalog {
    private Catalog() {}

    public static List<CatalogItem> items() {
        return new ArrayList<>(Arrays.asList(
            new CatalogItem("Solo Leveling", "Anime", 2024, "Fast binge", "A low-ranked hunter gains an unusual path to power and rapidly becomes stronger.", "action", "dark", "fantasy", "power-growth", "serious", "fast-paced"),
            new CatalogItem("Attack on Titan", "Anime", 2013, "Long series", "Humanity fights for survival while a much larger mystery slowly unfolds.", "action", "dark", "mystery", "world-building", "serious", "plot-twists"),
            new CatalogItem("Frieren: Beyond Journey's End", "Anime", 2023, "Medium series", "An elven mage reflects on time, friendship and what happens after the great adventure.", "fantasy", "world-building", "emotional", "character-growth", "slow-burn", "serious"),
            new CatalogItem("Vinland Saga", "Anime", 2019, "Medium series", "A violent coming-of-age story transforms into a deeper story about purpose and change.", "action", "historical", "dark", "character-growth", "serious", "emotional"),
            new CatalogItem("Jujutsu Kaisen", "Anime", 2020, "Medium series", "Sorcerers battle curses in a stylish, fast-moving supernatural action story.", "action", "supernatural", "dark", "fast-paced", "powers", "team"),
            new CatalogItem("Severance", "Series", 2022, "Short seasons", "Office workers undergo a procedure that separates work memories from personal life.", "mystery", "sci-fi", "slow-burn", "serious", "plot-twists", "psychological"),
            new CatalogItem("Silo", "Series", 2023, "Medium series", "Thousands live underground under strict rules while the truth about their world is questioned.", "mystery", "sci-fi", "world-building", "serious", "slow-burn", "plot-twists"),
            new CatalogItem("The Last of Us", "Series", 2023, "Short seasons", "Two survivors cross a devastated America and form an unlikely bond.", "dark", "emotional", "survival", "serious", "character-growth", "action"),
            new CatalogItem("Shōgun", "Series", 2024, "Limited-style", "Political conflict and cultural collision unfold in feudal Japan.", "historical", "political", "serious", "world-building", "slow-burn", "strategy"),
            new CatalogItem("Dune: Part Two", "Movie", 2024, "2h 46m", "A young leader embraces a dangerous destiny amid war, prophecy and political conflict.", "sci-fi", "world-building", "action", "political", "serious", "epic"),
            new CatalogItem("The Batman", "Movie", 2022, "2h 56m", "A young Batman investigates a serial killer targeting Gotham's elite.", "dark", "mystery", "crime", "serious", "detective", "slow-burn"),
            new CatalogItem("Edge of Tomorrow", "Movie", 2014, "1h 53m", "A soldier relives the same battle and learns to become increasingly effective.", "action", "sci-fi", "fast-paced", "power-growth", "time-loop", "strategy"),
            new CatalogItem("Prisoners", "Movie", 2013, "2h 33m", "A disappearance drives two men down increasingly desperate paths.", "mystery", "dark", "crime", "psychological", "serious", "slow-burn"),
            new CatalogItem("Arcane", "Series", 2021, "2 seasons", "Two sisters are pulled apart by conflict in a richly built fantasy-tech city.", "animation", "action", "world-building", "emotional", "character-growth", "dark"),
            new CatalogItem("Blue Eye Samurai", "Series", 2023, "1 season", "A master swordswoman pursues revenge through Edo-period Japan.", "action", "historical", "dark", "revenge", "serious", "animation"),
            new CatalogItem("Cyberpunk: Edgerunners", "Anime", 2022, "10 episodes", "A street kid enters a dangerous world of mercenaries and body modification.", "action", "sci-fi", "dark", "fast-paced", "emotional", "tragic")
        ));
    }
}
