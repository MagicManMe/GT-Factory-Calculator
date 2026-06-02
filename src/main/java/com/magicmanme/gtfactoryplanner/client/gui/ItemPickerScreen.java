package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.function.Consumer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.CustomModularScreen;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.ScrollingTextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.magicmanme.gtfactoryplanner.GTFactoryPlanner;
import com.magicmanme.gtfactoryplanner.client.SearchCatalog;
import com.magicmanme.gtfactoryplanner.data.ResourceKey;

/**
 * Searchable product picker: type to filter, click [>] to choose. The result
 * list is rebuilt per keystroke, capped at {@link #MAX_RESULTS} rows (searching
 * ~100k products per frame via enabled-predicates would be too slow).
 */
public class ItemPickerScreen extends CustomModularScreen {

    private static final int MAX_RESULTS = 100;

    private final Consumer<ResourceKey> onPick;
    private String query = "";
    @SuppressWarnings("rawtypes")
    private ListWidget resultList;

    public ItemPickerScreen(Consumer<ResourceKey> onPick) {
        super(GTFactoryPlanner.MODID);
        this.onPick = onPick;
    }

    @Override
    public ModularPanel buildUI(ModularGuiContext context) {
        resultList = new ListWidget<>().widthRel(1f)
            .expanded();
        rebuildResults();

        return ModularPanel.defaultPanel("item_picker", 260, 230)
            .padding(7)
            .child(
                Flow.column()
                    .sizeRel(1f)
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.START)
                    .child(
                        IKey.str("§lChoose a product")
                            .asWidget())
                    .child(new TextFieldWidget().value(new StringValue.Dynamic(() -> query, text -> {
                        if (!text.equals(query)) {
                            query = text;
                            rebuildResults();
                        }
                    }))
                        .autoUpdateOnChange(true)
                        .widthRel(1f)
                        .height(14))
                    .child(resultList)
                    .child(
                        new ButtonWidget<>().width(50)
                            .height(14)
                            .overlay(IKey.str("Cancel"))
                            .onMousePressed(b -> {
                                PlannerScreen.reopen();
                                return true;
                            })));
    }

    @SuppressWarnings("unchecked")
    private void rebuildResults() {
        resultList.removeAll();
        for (SearchCatalog.Entry entry : SearchCatalog.search(query, MAX_RESULTS)) {
            resultList.child(resultRow(entry));
        }
    }

    private IWidget resultRow(SearchCatalog.Entry entry) {
        return Flow.row()
            .widthRel(1f)
            .height(18)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(UiHelpers.icon(entry.key))
            .child(
                new ScrollingTextWidget(IKey.str(entry.displayName)).expanded()
                    .height(12))
            .child(
                new ButtonWidget<>().width(14)
                    .height(14)
                    .overlay(IKey.str("§2>"))
                    .onMousePressed(b -> {
                        onPick.accept(entry.key);
                        return true;
                    }));
    }
}
