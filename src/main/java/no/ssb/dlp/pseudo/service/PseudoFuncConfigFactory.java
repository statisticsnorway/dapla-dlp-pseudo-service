package no.ssb.dlp.pseudo.service;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.fpe.Alphabets;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ssb.dapla.dlp.pseudo.func.fpe.Alphabets.alphabetNameOf;
import static no.ssb.dapla.dlp.pseudo.func.text.CharacterGroup.ALPHANUMERIC;
import static no.ssb.dapla.dlp.pseudo.func.text.CharacterGroup.ALPHANUMERIC_NO;
import static no.ssb.dapla.dlp.pseudo.func.text.CharacterGroup.DIGITS;
import static no.ssb.dapla.dlp.pseudo.func.text.CharacterGroup.SYMBOLS;
import static no.ssb.dapla.dlp.pseudo.func.text.CharacterGroup.WHITESPACE;

// TODO: Move or remove presets

@Slf4j
class PseudoFuncConfigFactory {

    private static final Map<String, PseudoFuncConfigPreset> PSEUDO_CONFIG_PRESETS_MAP = new HashMap<>();

    static {
        PSEUDO_CONFIG_PRESETS_MAP.putAll(Maps.uniqueIndex(ImmutableList.of(
          fpePseudoFuncConfigPreset("fpe-text", alphabetNameOf(ALPHANUMERIC, WHITESPACE, SYMBOLS)),
          fpePseudoFuncConfigPreset("fpe-text_no", alphabetNameOf(ALPHANUMERIC_NO, WHITESPACE, SYMBOLS)),
          fpePseudoFuncConfigPreset("fpe-fnr", alphabetNameOf(DIGITS))
        ), PseudoFuncConfigPreset::getFuncName));
    }

    private PseudoFuncConfigFactory() {}

    private static PseudoFuncConfigPreset fpePseudoFuncConfigPreset(String funcName, String alphabet) {
        if (!funcName.startsWith("fpe-")) {
            throw new IllegalArgumentException("FPE functions must be prefixed with 'fpe-'");
        }

        return new PseudoFuncConfigPreset(funcName, ImmutableMap.of(
          PseudoFuncConfig.Param.FUNC_IMPL, FpeFunc.class.getName(),
          FpeFuncConfig.Param.ALPHABET, alphabet
        ), ImmutableList.of(FpeFuncConfig.Param.KEY_ID));
    }

    static PseudoFuncConfigPreset getConfigPreset(FuncDeclaration funcDecl) {
        String funcName = funcDecl.getFuncName();
        PseudoFuncConfigPreset preset = PSEUDO_CONFIG_PRESETS_MAP.get(funcDecl.getFuncName());
        if (preset != null) {
            return preset;
        }

        /*
        If no preset was defined AND this is an FPE function, then create the preset dynamically
        The alphabet to be used will be deduced from the function name (+ separated string with references to
        any already defined CharacterGroup, see no.ssb.dapla.dlp.pseudo.func.fpe.Alphabets)
         */
        if (funcName.startsWith("fpe-")) {
            String alphabetName = funcDecl.getFuncName().substring(4);
            log.info("Add dynamic FPE function preset '{}' with alphabet '{}'. Allowed characters: {}",
              funcName, alphabetName, new String(Alphabets.fromAlphabetName(alphabetName).availableCharacters()));
            preset = fpePseudoFuncConfigPreset(funcName, alphabetName);
            PSEUDO_CONFIG_PRESETS_MAP.put(funcName, preset);
            return preset;
        }

        throw new InvalidPseudoFuncDeclarationException(funcName, "Check spelling. Only FPE functions can be created dynamically");
    }

    public static PseudoFuncConfig get(String funcDeclarationString) {
        FuncDeclaration funcDecl = FuncDeclaration.fromString(funcDeclarationString);
        return getConfigPreset(funcDecl).toPseudoFuncConfig(funcDecl);
    }

    /**
     * A PseudoFuncConfig preset that holds a set of common parameters + defines a set of parameters that must be
     * supplied externally in order to create a valid PseudoFuncConfig.
     */
    static class PseudoFuncConfigPreset {
        private final String funcName;

        private final Map<String, String> defaultParams;

        private final List<String> userDefinedParams;

        public PseudoFuncConfigPreset(String funcName, Map<String, String> defaultParams, List<String> additionalParams) {
            this.funcName = funcName;
            this.defaultParams = (defaultParams != null) ? defaultParams : Collections.emptyMap();
            this.userDefinedParams = (additionalParams != null) ? additionalParams : Collections.emptyList();
        }

        public String getFuncName() {
            return funcName;
        }

        /**
         * Params common for all functions
         */
        public Map<String, String> getDefaultParams() {
            return defaultParams;
        }

        /**
         * Names of params that must be supplied externally. Note that ordering is important.
         */
        public List<String> getUserDefinedParams() {
            return userDefinedParams;
        }

        /**
         * Construct a PseudoFuncConfig using the supplied user defined arguments
         *
         * @throws PseudoFuncConfigException if args does not satisfy expected user defined parameters
         */
        public PseudoFuncConfig toPseudoFuncConfig(FuncDeclaration funcDecl) {
            List<String> args = funcDecl.getArgs();
            if (args == null) {
                args = Collections.emptyList();
            }
            if (args.size() != userDefinedParams.size()) {
                throw new PseudoFuncConfigException("Error creating PseudoFuncConfig for '" + funcName + "'. Expected arguments for " + userDefinedParams + ", but got: " + args);
            }
            ImmutableMap.Builder params = ImmutableMap.builder();
            params.put(PseudoFuncConfig.Param.FUNC_DECL, funcDecl.toString());
            params.putAll(defaultParams);
            for (int i = 0; i < userDefinedParams.size(); i++) {
                params.put(userDefinedParams.get(i), args.get(i));
            }

            return new PseudoFuncConfig(params.build());
        }
    }

    static class FuncDeclaration {
        private static final Splitter COMMA_SPLITTER = Splitter.on(",").trimResults();

        private final String funcName;
        private final List<String> args;

        private FuncDeclaration(String funcName, List<String> args) {
            this.funcName = funcName;
            this.args = args;
        }

        public static FuncDeclaration fromString(String s) {
            s = s.replaceAll("[()]", " ").trim();
            if (s.indexOf(' ') == -1) {
                return new FuncDeclaration(s, Collections.emptyList());
            } else {
                String funcName = s.substring(0, s.indexOf(' '));
                String argsString = s.substring(funcName.length(), s.length());
                List<String> args = COMMA_SPLITTER.splitToList(argsString);
                return new FuncDeclaration(funcName, args);
            }
        }

        @Override
        public String toString() {
            return funcName + "(" + String.join(",", args) + ")";
        }

        public String getFuncName() {
            return funcName;
        }

        public List<String> getArgs() {
            return args;
        }
    }

    static class InvalidPseudoFuncDeclarationException extends PseudoException {
        public InvalidPseudoFuncDeclarationException(String funcName, String message) {
            super("Invalid pseudo function declaration '" + funcName + "': " + message);
        }
    }

    static class PseudoFuncNotDefinedException extends PseudoException {
        public PseudoFuncNotDefinedException(String funcName) {
            super("No pseudo func '" + funcName + "' has been defined. Check spelling or supply a function definition to PseudoFuncConfigFactory");
        }
    }

    static class PseudoFuncConfigException extends PseudoException {
        public PseudoFuncConfigException(String message) {
            super(message);
        }
    }


}
