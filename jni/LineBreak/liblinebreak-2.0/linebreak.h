#ifndef LINEBREAK_H
#define LINEBREAK_H

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

#define LINEBREAK_VERSION	0x0195	/**< Version of the library linebreak */
extern const int linebreak_version;

#ifndef LINEBREAK_UTF_TYPES_DEFINED
#define LINEBREAK_UTF_TYPES_DEFINED
typedef unsigned char	utf8_t;		/**< Type for UTF-8 data points */
typedef unsigned short	utf16_t;	/**< Type for UTF-16 data points */
typedef unsigned int	utf32_t;	/**< Type for UTF-32 data points */
#endif

#define LINEBREAK_MUSTBREAK		0	/**< Break is mandatory */
#define LINEBREAK_ALLOWBREAK	1	/**< Break is allowed */
#define LINEBREAK_NOBREAK		2	/**< No break is possible */
#define LINEBREAK_INSIDEACHAR	3	/**< A UTF-8/16 sequence is unfinished */

void init_linebreak(void);
void set_linebreaks_utf8(const utf8_t *s, size_t len, const char* lang, char *brks);
void set_linebreaks_utf16(const utf16_t *s, size_t len, const char* lang, char *brks);
void set_linebreaks_utf32(const utf32_t *s, size_t len, const char* lang, char *brks);
int is_line_breakable(utf32_t char1, utf32_t char2, const char* lang);

#ifdef __cplusplus
}
#endif

#endif /* LINEBREAK_H */
