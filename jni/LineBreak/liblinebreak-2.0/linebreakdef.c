#include "linebreak.h"
#include "linebreakdef.h"

/**
 * English-specifc data over the default Unicode rules.
 */
static struct LineBreakProperties lb_prop_English[] = {
	{ 0x2018, 0x2018, LBP_OP },	/* Left single quotation mark: opening */
	{ 0x201C, 0x201C, LBP_OP },	/* Left double quotation mark: opening */
	{ 0x201D, 0x201D, LBP_CL },	/* Right double quotation mark: closing */
	{ 0, 0, LBP_Undefined }
};

/**
 * German-specifc data over the default Unicode rules.
 */
static struct LineBreakProperties lb_prop_German[] = {
	{ 0x00AB, 0x00AB, LBP_CL },	/* Left double angle quotation mark: closing */
	{ 0x00BB, 0x00BB, LBP_OP },	/* Right double angle quotation mark: opening */
	{ 0x2018, 0x2018, LBP_CL },	/* Left single quotation mark: closing */
	{ 0x201C, 0x201C, LBP_CL },	/* Left double quotation mark: closing */
	{ 0x2039, 0x2039, LBP_CL },	/* Left single angle quotation mark: closing */
	{ 0x203A, 0x203A, LBP_OP },	/* Right single angle quotation mark: opening */
	{ 0, 0, LBP_Undefined }
};

/**
 * Spanish-specifc data over the default Unicode rules.
 */
static struct LineBreakProperties lb_prop_Spanish[] = {
	{ 0x00AB, 0x00AB, LBP_OP },	/* Left double angle quotation mark: opening */
	{ 0x00BB, 0x00BB, LBP_CL },	/* Right double angle quotation mark: closing */
	{ 0x2018, 0x2018, LBP_OP },	/* Left single quotation mark: opening */
	{ 0x201C, 0x201C, LBP_OP },	/* Left double quotation mark: opening */
	{ 0x201D, 0x201D, LBP_CL },	/* Right double quotation mark: closing */
	{ 0x2039, 0x2039, LBP_OP },	/* Left single angle quotation mark: opening */
	{ 0x203A, 0x203A, LBP_CL },	/* Right single angle quotation mark: closing */
	{ 0, 0, LBP_Undefined }
};

/**
 * French-specifc data over the default Unicode rules.
 */
static struct LineBreakProperties lb_prop_French[] = {
	{ 0x00AB, 0x00AB, LBP_OP },	/* Left double angle quotation mark: opening */
	{ 0x00BB, 0x00BB, LBP_CL },	/* Right double angle quotation mark: closing */
	{ 0x2018, 0x2018, LBP_OP },	/* Left single quotation mark: opening */
	{ 0x201C, 0x201C, LBP_OP },	/* Left double quotation mark: opening */
	{ 0x201D, 0x201D, LBP_CL },	/* Right double quotation mark: closing */
	{ 0x2039, 0x2039, LBP_OP },	/* Left single angle quotation mark: opening */
	{ 0x203A, 0x203A, LBP_CL },	/* Right single angle quotation mark: closing */
	{ 0, 0, LBP_Undefined }
};

/**
 * Russian-specifc data over the default Unicode rules.
 */
static struct LineBreakProperties lb_prop_Russian[] = {
	{ 0x00AB, 0x00AB, LBP_OP },	/* Left double angle quotation mark: opening */
	{ 0x00BB, 0x00BB, LBP_CL },	/* Right double angle quotation mark: closing */
	{ 0x201C, 0x201C, LBP_CL },	/* Left double quotation mark: closing */
	{ 0, 0, LBP_Undefined }
};

/**
 * Chinese-specifc data over the default Unicode rules.
 */
static struct LineBreakProperties lb_prop_Chinese[] = {
	{ 0x2018, 0x2018, LBP_OP },	/* Left single quotation mark: opening */
	{ 0x2019, 0x2019, LBP_CL },	/* Right single quotation mark: closing */
	{ 0x201C, 0x201C, LBP_OP },	/* Left double quotation mark: opening */
	{ 0x201D, 0x201D, LBP_CL },	/* Right double quotation mark: closing */
	{ 0, 0, LBP_Undefined }
};

/**
 * Association data of language-specific line breaking properties with
 * language names.  This is the definition for the static data in this
 * file.  If you want more flexibility, or do not need the data here,
 * you may want to redefine \e lb_prop_lang_map in your C source file.
 */
struct LineBreakPropertiesLang lb_prop_lang_map[] = {
	{ "en", 2, lb_prop_English },
	{ "de", 2, lb_prop_German },
	{ "es", 2, lb_prop_Spanish },
	{ "fr", 2, lb_prop_French },
	{ "ru", 2, lb_prop_Russian },
	{ "zh", 2, lb_prop_Chinese },
	{ NULL, 0, NULL }
};
