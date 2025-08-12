package voiidstudios.vct.managers;

import voiidstudios.vct.VoiidCountdownTimer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DynamicsManager {
    public static final String DYNAMIC_KEY = "dynamic";
    public static final String DYNAMIC_GRAD_KEY = "dynamic-g";
    public static final String NEG_DYNAMIC_KEY = "-dynamic";
    public static final String NEG_DYNAMIC_GRAD_KEY = "-dynamic-g";

    private static final int maxIndex = 10;
    private int mmIndex = maxIndex;

    private BigDecimal miniGradientIndexBD = new BigDecimal("-1.0");
    private final BigDecimal stepBD = new BigDecimal("0.1");
    private final BigDecimal one = new BigDecimal("1.0");
    private final BigDecimal minusOne = new BigDecimal("-1.0");

    private final Map<String, String> formattedDynamicsValues = new HashMap<>();
    private final DecimalFormat decimalFormat;

    private final VoiidCountdownTimer plugin;

    public DynamicsManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.#", symbols);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        startIndexTask();
    }

    private void startIndexTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            mmIndex -= 1;
            if (mmIndex == 1) mmIndex = maxIndex;

            miniGradientIndexBD = miniGradientIndexBD.add(stepBD);
            if (miniGradientIndexBD.compareTo(one) > 0) {
                miniGradientIndexBD = new BigDecimal("-1.0");
            }

            updateFormattedDynamicsValues();
        }, 0, 1);
    }

    private void updateFormattedDynamicsValues() {
        final String mmIndexStr = Integer.toString(mmIndex);
        final String phaseMmGStr = decimalFormat.format(miniGradientIndexBD);
        final String negMmIndexStr = Integer.toString(maxIndex - mmIndex);
        final String negPhaseMmGStr = decimalFormat.format(miniGradientIndexBD.multiply(minusOne));

        formattedDynamicsValues.put(DYNAMIC_KEY, mmIndexStr);
        formattedDynamicsValues.put(DYNAMIC_GRAD_KEY, phaseMmGStr);
        formattedDynamicsValues.put(NEG_DYNAMIC_KEY, negMmIndexStr);
        formattedDynamicsValues.put(NEG_DYNAMIC_GRAD_KEY, negPhaseMmGStr);
    }

    public String formatPhases(String value) {
        return value
                .replace("#"+DYNAMIC_GRAD_KEY+"#", formattedDynamicsValues.getOrDefault(DYNAMIC_GRAD_KEY, ""))
                .replace("#"+NEG_DYNAMIC_GRAD_KEY+"#", formattedDynamicsValues.getOrDefault(NEG_DYNAMIC_GRAD_KEY, ""))
                .replace("#"+DYNAMIC_KEY+"#", formattedDynamicsValues.getOrDefault(DYNAMIC_KEY, ""))
                .replace("#"+NEG_DYNAMIC_KEY+"#", formattedDynamicsValues.getOrDefault(NEG_DYNAMIC_KEY, ""));
    }
}
