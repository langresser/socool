/**
 * Constant value to mark the end of string.  It is not a valid Unicode
 * character.
 */
#define EOS 0xFFFF

/**
 * Line break classes.  This is a direct mapping of Table 1 of Unicode
 * Standard Annex 14, Revision 19.
 */
enum LineBreakClass
{
	/* This is used to signal an error condition. */
	LBP_Undefined,	/**< Undefined */

	/* The following break classes are treated in the pair table. */
	LBP_OP,			/**< Opening punctuation */
	LBP_CL,			/**< Closing punctuation */
	LBP_CP,			/**< Closing parenthesis */
	LBP_QU,			/**< Ambiguous quotation */
	LBP_GL,			/**< Glue */
	LBP_NS,			/**< Non-starters */
	LBP_EX,			/**< Exclamation/Interrogation */
	LBP_SY,			/**< Symbols allowing break after */
	LBP_IS,			/**< Infix separator */
	LBP_PR,			/**< Prefix */
	LBP_PO,			/**< Postfix */
	LBP_NU,			/**< Numeric */
	LBP_AL,			/**< Alphabetic */
	LBP_ID,			/**< Ideographic */
	LBP_IN,			/**< Inseparable characters */
	LBP_HY,			/**< Hyphen */
	LBP_BA,			/**< Break after */
	LBP_BB,			/**< Break before */
	LBP_B2,			/**< Break on either side (but not pair) */
	LBP_ZW,			/**< Zero-width space */
	LBP_CM,			/**< Combining marks */
	LBP_WJ,			/**< Word joiner */
	LBP_H2,			/**< Hangul LV */
	LBP_H3,			/**< Hangul LVT */
	LBP_JL,			/**< Hangul L Jamo */
	LBP_JV,			/**< Hangul V Jamo */
	LBP_JT,			/**< Hangul T Jamo */

	/* The following break classes are not treated in the pair table */
	LBP_AI,			/**< Ambiguous (alphabetic or ideograph) */
	LBP_BK,			/**< Break (mandatory) */
	LBP_CB,			/**< Contingent break */
	LBP_CR,			/**< Carriage return */
	LBP_LF,			/**< Line feed */
	LBP_NL,			/**< Next line */
	LBP_SA,			/**< South-East Asian */
	LBP_SG,			/**< Surrogates */
	LBP_SP,			/**< Space */
	LBP_XX			/**< Unknown */
};

/**
 * Struct for entries of line break properties.  The array of the
 * entries \e must be sorted.
 */
struct LineBreakProperties
{
	utf32_t start;				/**< Starting coding point */
	utf32_t end;				/**< End coding point */
	enum LineBreakClass prop;	/**< The line breaking property */
};

/**
 * Struct for association of language-specific line breaking properties
 * with language names.
 */
struct LineBreakPropertiesLang
{
	const char *lang;					/**< Language name */
	size_t namelen;						/**< Length of name to match */
	struct LineBreakProperties *lbp;	/**< Pointer to associated data */
};

/**
 * Abstract function interface for #lb_get_next_char_utf8,
 * #lb_get_next_char_utf16, and #lb_get_next_char_utf32.
 */
typedef utf32_t (*get_next_char_t)(const void *, size_t, size_t *);

/* Declarations */
extern struct LineBreakProperties lb_prop_default[];
extern struct LineBreakPropertiesLang lb_prop_lang_map[];

/* Function Prototype */
utf32_t lb_get_next_char_utf8(const utf8_t *s, size_t len, size_t *ip);
utf32_t lb_get_next_char_utf16(const utf16_t *s, size_t len, size_t *ip);
utf32_t lb_get_next_char_utf32(const utf32_t *s, size_t len, size_t *ip);
void set_linebreaks(
		const void *s,
		size_t len,
		const char *lang,
		char *brks,
		get_next_char_t get_next_char);
