// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import zombie.UsedFromLua;
import zombie.core.Translator;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.entity.components.crafting.InputScript;
import zombie.scripting.objects.CraftRecipeCategory;

@UsedFromLua
public class DebugCSVExport {
    public static void doCSV() throws IOException {
        String CSV_SEPARATOR = ",";
        String DELIMITER = "; ";
        FileWriter fw = new FileWriter("TailoringCSV.txt");
        StringBuilder result = new StringBuilder();
        result.append("RECIPE ID,RECIPE NAME,SKILLS,CONSUMED ITEMS,KEEP ITEMS,XP AWARD,NEED TO BE LEARN,AUTO LEARN\n");
        int index = 0;

        for (CraftRecipe recipe : ScriptManager.instance.getAllCraftRecipes()) {
            if (recipe.getCategory().equals(CraftRecipeCategory.TAILORING.toString())) {
                index++;
                StringBuilder skill = new StringBuilder();
                StringBuilder xpAwards = new StringBuilder();
                List<String> consumedItems = new ArrayList<>();
                List<String> keepItems = new ArrayList<>();
                List<String> autoLearn = new ArrayList<>();
                if (recipe.skillRequired != null) {
                    String skills = recipe.skillRequired
                        .stream()
                        .map(rs -> "%s:%s".formatted(rs.getPerk().getName(), rs.getLevel()))
                        .collect(Collectors.joining("; "));
                    skill.append(skills);
                }

                for (InputScript input : recipe.getInputs()) {
                    if (!input.isKeep()) {
                        StringBuilder itemOneLine = new StringBuilder();
                        itemOneLine.append(
                            input.getPossibleInputItems()
                                .stream()
                                .map(v -> v.getScriptObjectFullType().replaceAll("Base.", "").replaceAll("([a-z])([0-9A-Z])", "$1_$2").toUpperCase(Locale.ROOT))
                                .collect(Collectors.joining("; "))
                        );
                        consumedItems.add("%s:%s".formatted(itemOneLine, input.getAmount()));
                    } else {
                        StringBuilder itemOneLine = new StringBuilder();
                        if (input.getItemTags().isEmpty()) {
                            itemOneLine.append(
                                input.getPossibleInputItems()
                                    .stream()
                                    .map(
                                        v -> v.getDisplayName().contains("(")
                                            ? v.getDisplayName().substring(0, v.getDisplayName().indexOf(40) - 1)
                                            : v.getDisplayName()
                                    )
                                    .collect(Collectors.joining("; "))
                            );
                        } else {
                            itemOneLine.append("TAGS: ").append(input.getItemTags().stream().map(Object::toString).collect(Collectors.joining("; ")));
                        }

                        keepItems.add("%s:%s".formatted(itemOneLine, input.getAmount()));
                    }
                }

                if (recipe.autoLearnAll != null) {
                    autoLearn.add(
                        recipe.autoLearnAll.stream().map(v -> "%s:%s".formatted(v.getPerk().getName(), v.getLevel())).collect(Collectors.joining("; "))
                    );
                }

                if (recipe.xpAward != null) {
                    xpAwards.append(
                        recipe.xpAward.stream().map(xp -> "%s:%s".formatted(xp.getPerk().getName(), xp.getAmount())).collect(Collectors.joining("; "))
                    );
                }

                result.append(
                    "%s%s%s%s%s%s%s%s\n"
                        .formatted(
                            recipe.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase(Locale.ROOT),
                            "," + Translator.getRecipeName(recipe.getName()),
                            "," + skill,
                            "," + consumedItems.stream().map(String::toString).collect(Collectors.joining(" AND ")),
                            "," + String.join("; ", keepItems),
                            "," + xpAwards,
                            "," + recipe.needToBeLearn(),
                            "," + String.join("; ", autoLearn)
                        )
                );
            }
        }

        fw.write(result.toString());
        fw.flush();
        fw.close();
    }
}
