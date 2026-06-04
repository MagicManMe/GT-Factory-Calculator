package com.magicmanme.gtfactoryplanner.client.gui;

import java.util.function.Consumer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
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
 *
 * Note: extends ModularScreen with a panel-builder lambda capturing constructor
 * parameters. Do NOT use CustomModularScreen with instance fields here — the
 * super constructor invokes buildUI before subclass fields are assigned.
 */
public class ItemPickerScreen extends ModularScreen {

    private static final int MAX_RESULTS = 100;

    public ItemPickerScreen(Consumer<ResourceKey> onPick) {
        super(GTFactoryPlanner.MODID, context -> buildPanel(onPick));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static ModularPanel buildPanel(Consumer<ResourceKey> onPick) {
        // Mutable state shared by the search field and the rebuild action; held in
        // locals (captured by lambdas) rather than screen fields, see class note.
        String[] query = { "" };
        ListWidget resultList = (ListWidget) new ListWidget<>().widthRel(1f)
            .expanded();

        Runnable rebuild = () -> {
            resultList.removeAll();
            for (SearchCatalog.Entry entry : SearchCatalog.search(query[0], MAX_RESULTS)) {
                resultList.child(resultRow(entry, onPick));
            }
        };
        rebuild.run();

        return ModularPanel.defaultPanel("item_picker", 260, 230)
            .padding(7)
            .child(
                Flow.column()
                    .sizeRel(1f)
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.START)
                    .child(
                        IKey.str("§lChoose a product")
                            .asWidget()
                            .height(10))
                    .child(UiHelpers.divider())
                    .child(new TextFieldWidget().value(new StringValue.Dynamic(() -> query[0], text -> {
                        if (!text.equals(query[0])) {
                            query[0] = text;
                            rebuild.run();
                        }
                    }))
                        .autoUpdateOnChange(true)
                        .widthRel(1f)
                        .height(14))
                    .child(resultList)
                    .child(UiHelpers.divider())
                    .child(
                        new ButtonWidget<>().width(56)
                            .height(UiHelpers.BTN)
                            .overlay(IKey.str("Cancel"))
                            .onMousePressed(b -> {
                                PlannerScreen.reopen();
                                return true;
                            })));
    }

    private static IWidget resultRow(SearchCatalog.Entry entry, Consumer<ResourceKey> onPick) {
        return Flow.row()
            .widthRel(1f)
            .height(UiHelpers.ROW_H)
            .childPadding(3)
            .crossAxisAlignment(Alignment.CrossAxis.CENTER)
            .child(UiHelpers.icon(entry.key))
            .child(
                new ScrollingTextWidget(IKey.str(entry.displayName)).expanded()
                    .height(12))
            .child(
                new ButtonWidget<>().size(UiHelpers.BTN)
                    .overlay(IKey.str("§2+"))
                    .addTooltipLine("Use this product")
                    .onMousePressed(b -> {
                        onPick.accept(entry.key);
                        return true;
                    }));
    }
}
