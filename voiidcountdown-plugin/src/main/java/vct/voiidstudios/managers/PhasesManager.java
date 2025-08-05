package vct.voiidstudios.managers;

import vct.voiidstudios.VoiidCountdownTimer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class PhasesManager {
    public static final String PHASE_MM_KEY = "phase-mm";
    public static final String PHASE_MM_G_KEY = "phase-mm-g";
    public static final String NEG_PHASE_MM_KEY = "-phase-mm";
    public static final String NEG_PHASE_MM_G_KEY = "-phase-mm-g";

    private static final int maxIndex = 16777215;
    private static final int maxMIndex = 10;

    private int index = maxIndex;
    private int mmIndex = maxMIndex;

    private BigDecimal miniGradientIndexBD = new BigDecimal("-1.0");
    private final BigDecimal stepBD = new BigDecimal("0.1");
    private final BigDecimal one = new BigDecimal("1.0");
    private final BigDecimal minusOne = new BigDecimal("-1.0");

    private final Map<String, String> formattedPhaseValues = new HashMap<>();
    private final DecimalFormat decimalFormat;

    private final VoiidCountdownTimer plugin;

    public PhasesManager(VoiidCountdownTimer plugin) {
        this.plugin = plugin;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("#.#", symbols);
        decimalFormat.setRoundingMode(RoundingMode.DOWN);

        startIndexTask();
    }

    private void startIndexTask() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            index -= 1;
            if (index == 0) index = maxIndex;

            mmIndex -= 1;
            if (mmIndex == 1) mmIndex = maxMIndex;

            miniGradientIndexBD = miniGradientIndexBD.add(stepBD);
            if (miniGradientIndexBD.compareTo(one) > 0) {
                miniGradientIndexBD = new BigDecimal("-1.0");
            }

            updateFormattedPhaseValues();
        }, 0, 1);
    }

    private void updateFormattedPhaseValues() {
        final String mmIndexStr = Integer.toString(mmIndex);
        final String phaseMmGStr = decimalFormat.format(miniGradientIndexBD);
        final String negMmIndexStr = Integer.toString(maxMIndex - mmIndex);
        final String negPhaseMmGStr = decimalFormat.format(miniGradientIndexBD.multiply(minusOne));

        formattedPhaseValues.put(PHASE_MM_KEY, mmIndexStr);
        formattedPhaseValues.put(PHASE_MM_G_KEY, phaseMmGStr);
        formattedPhaseValues.put(NEG_PHASE_MM_KEY, negMmIndexStr);
        formattedPhaseValues.put(NEG_PHASE_MM_G_KEY, negPhaseMmGStr);
    }

    public String formatPhases(String value) {
        return value
                .replace("#phase-mm-g#", formattedPhaseValues.getOrDefault(PHASE_MM_G_KEY, ""))
                .replace("#-phase-mm-g#", formattedPhaseValues.getOrDefault(NEG_PHASE_MM_G_KEY, ""))
                .replace("#phase-mm#", formattedPhaseValues.getOrDefault(PHASE_MM_KEY, ""))
                .replace("#-phase-mm#", formattedPhaseValues.getOrDefault(NEG_PHASE_MM_KEY, ""));
    }
}
