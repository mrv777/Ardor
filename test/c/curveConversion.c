
/* Montgomery <-> Edwards isomorphism
 * Daniel Beer <dlbeer@gmail.com>, 18 Jan 2014
 *
 * This file is in the public domain.
 */


#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define F25519_SIZE  32

extern const uint8_t f25519_zero[F25519_SIZE];
extern const uint8_t f25519_one[F25519_SIZE];

uint8_t* datahex(char* string) {

    if(string == NULL) 
       return NULL;

    size_t slength = strlen(string);
    if((slength % 2) != 0) // must be even
       return NULL;

    size_t dlength = slength / 2;

    uint8_t* data = malloc(dlength);
    memset(data, 0, dlength);

    size_t index = 0;
    while (index < slength) {
        char c = string[index];
        int value = 0;
        if(c >= '0' && c <= '9')
          value = (c - '0');
        else if (c >= 'A' && c <= 'F') 
          value = (10 + (c - 'A'));
        else if (c >= 'a' && c <= 'f')
          value = (10 + (c - 'a'));
        else {
          free(data);
          return NULL;
        }

        data[(index/2)] += value << (((index + 1) % 2) * 4);

        index++;
    }

    return data;
}

void tohex(uint8_t* inp) {
	int i;
	for (i = 0; i < 32; i++) {
		printf("%02X", inp[i]);
	}
	printf("\n");	
}


/* Copy two points */
static inline void f25519_copy(uint8_t *x, const uint8_t *a)
{
	memcpy(x, a, F25519_SIZE);
}


const uint8_t f25519_zero[F25519_SIZE] = {0};
const uint8_t f25519_one[F25519_SIZE] = {1};

void f25519_load(uint8_t *x, uint32_t c)
{
	unsigned int i;

	for (i = 0; i < sizeof(c); i++) {
		x[i] = c;
		c >>= 8;
	}

	for (; i < F25519_SIZE; i++)
		x[i] = 0;
}

void f25519_select(uint8_t *dst,
		   const uint8_t *zero, const uint8_t *one,
		   uint8_t condition)
{
	const uint8_t mask = -condition;
	int i;

	for (i = 0; i < F25519_SIZE; i++)
		dst[i] = zero[i] ^ (mask & (one[i] ^ zero[i]));
}

void f25519_normalize(uint8_t *x)
{
	uint8_t minusp[F25519_SIZE];
	uint16_t c;
	int i;

	/* Reduce using 2^255 = 19 mod p */
	c = (x[31] >> 7) * 19;
	x[31] &= 127;

	for (i = 0; i < F25519_SIZE; i++) {
		c += x[i];
		x[i] = c;
		c >>= 8;
	}

	/* The number is now less than 2^255 + 18, and therefore less than
	 * 2p. Try subtracting p, and conditionally load the subtracted
	 * value if underflow did not occur.
	 */
	c = 19;

	for (i = 0; i + 1 < F25519_SIZE; i++) {
		c += x[i];
		minusp[i] = c;
		c >>= 8;
	}

	c += ((uint16_t)x[i]) - 128;
	minusp[31] = c;

	/* Load x-p if no underflow */
	f25519_select(x, minusp, x, (c >> 15) & 1);
}

uint8_t f25519_eq(const uint8_t *x, const uint8_t *y)
{
	uint8_t sum = 0;
	int i;

	for (i = 0; i < F25519_SIZE; i++)
		sum |= x[i] ^ y[i];

	sum |= (sum >> 4);
	sum |= (sum >> 2);
	sum |= (sum >> 1);

	return (sum ^ 1) & 1;
}

void f25519_add(uint8_t *r, const uint8_t *a, const uint8_t *b)
{
	uint16_t c = 0;
	int i;

	/* Add */
	for (i = 0; i < F25519_SIZE; i++) {
		c >>= 8;
		c += ((uint16_t)a[i]) + ((uint16_t)b[i]);
		r[i] = c;
	}

	/* Reduce with 2^255 = 19 mod p */
	r[31] &= 127;
	c = (c >> 7) * 19;

	for (i = 0; i < F25519_SIZE; i++) {
		c += r[i];
		r[i] = c;
		c >>= 8;
	}
}

void f25519_sub(uint8_t *r, const uint8_t *a, const uint8_t *b)
{
	uint32_t c = 0;
	int i;

	/* Calculate a + 2p - b, to avoid underflow */
	c = 218;
	for (i = 0; i + 1 < F25519_SIZE; i++) {
		c += 65280 + ((uint32_t)a[i]) - ((uint32_t)b[i]);
		r[i] = c;
		c >>= 8;
	}

	c += ((uint32_t)a[31]) - ((uint32_t)b[31]);
	r[31] = c & 127;
	c = (c >> 7) * 19;

	for (i = 0; i < F25519_SIZE; i++) {
		c += r[i];
		r[i] = c;
		c >>= 8;
	}
}

void f25519_neg(uint8_t *r, const uint8_t *a)
{
	uint32_t c = 0;
	int i;

	/* Calculate 2p - a, to avoid underflow */
	c = 218;
	for (i = 0; i + 1 < F25519_SIZE; i++) {
		c += 65280 - ((uint32_t)a[i]);
		r[i] = c;
		c >>= 8;
	}

	c -= ((uint32_t)a[31]);
	r[31] = c & 127;
	c = (c >> 7) * 19;

	for (i = 0; i < F25519_SIZE; i++) {
		c += r[i];
		r[i] = c;
		c >>= 8;
	}
}

void f25519_mul__distinct(uint8_t *r, const uint8_t *a, const uint8_t *b)
{
	uint32_t c = 0;
	int i;

	for (i = 0; i < F25519_SIZE; i++) {
		int j;

		c >>= 8;
		for (j = 0; j <= i; j++)
			c += ((uint32_t)a[j]) * ((uint32_t)b[i - j]);

		for (; j < F25519_SIZE; j++)
			c += ((uint32_t)a[j]) *
			     ((uint32_t)b[i + F25519_SIZE - j]) * 38;

		r[i] = c;
	}
	r[31] &= 127;
	c = (c >> 7) * 19;

	for (i = 0; i < F25519_SIZE; i++) {
		c += r[i];
		r[i] = c;
		c >>= 8;
	}
}

void f25519_mul(uint8_t *r, const uint8_t *a, const uint8_t *b)
{
	uint8_t tmp[F25519_SIZE];

	f25519_mul__distinct(tmp, a, b);
	f25519_copy(r, tmp);
}

void f25519_mul_c(uint8_t *r, const uint8_t *a, uint32_t b)
{
	uint32_t c = 0;
	int i;

	for (i = 0; i < F25519_SIZE; i++) {
		c >>= 8;
		c += b * ((uint32_t)a[i]);
		r[i] = c;
	}

	r[31] &= 127;
	c >>= 7;
	c *= 19;

	for (i = 0; i < F25519_SIZE; i++) {
		c += r[i];
		r[i] = c;
		c >>= 8;
	}
}

void f25519_inv__distinct(uint8_t *r, const uint8_t *x)
{
	uint8_t s[F25519_SIZE];
	int i;

	/* This is a prime field, so by Fermat's little theorem:
	 *
	 *     x^(p-1) = 1 mod p
	 *
	 * Therefore, raise to (p-2) = 2^255-21 to get a multiplicative
	 * inverse.
	 *
	 * This is a 255-bit binary number with the digits:
	 *
	 *     11111111... 01011
	 *
	 * We compute the result by the usual binary chain, but
	 * alternate between keeping the accumulator in r and s, so as
	 * to avoid copying temporaries.
	 */

	/* 1 1 */
	f25519_mul__distinct(s, x, x);
	f25519_mul__distinct(r, s, x);

	/* 1 x 248 */
	for (i = 0; i < 248; i++) {
		f25519_mul__distinct(s, r, r);
		f25519_mul__distinct(r, s, x);
	}

	/* 0 */
	f25519_mul__distinct(s, r, r);

	/* 1 */
	f25519_mul__distinct(r, s, s);
	f25519_mul__distinct(s, r, x);

	/* 0 */
	f25519_mul__distinct(r, s, s);

	/* 1 */
	f25519_mul__distinct(s, r, r);
	f25519_mul__distinct(r, s, x);

	/* 1 */
	f25519_mul__distinct(s, r, r);
	f25519_mul__distinct(r, s, x);
}

void f25519_inv(uint8_t *r, const uint8_t *x)
{
	uint8_t tmp[F25519_SIZE];

	f25519_inv__distinct(tmp, x);
	f25519_copy(r, tmp);
}

/* Raise x to the power of (p-5)/8 = 2^252-3, using s for temporary
 * storage.
 */
static void exp2523(uint8_t *r, const uint8_t *x, uint8_t *s)
{
	int i;

	/* This number is a 252-bit number with the binary expansion:
	 *
	 *     111111... 01
	 */

	/* 1 1 */
	f25519_mul__distinct(r, x, x);
	f25519_mul__distinct(s, r, x);

	/* 1 x 248 */
	for (i = 0; i < 248; i++) {
		f25519_mul__distinct(r, s, s);
		f25519_mul__distinct(s, r, x);
	}

	/* 0 */
	f25519_mul__distinct(r, s, s);

	/* 1 */
	f25519_mul__distinct(s, r, r);
	f25519_mul__distinct(r, s, x);
}

void f25519_sqrt(uint8_t *r, const uint8_t *a)
{
	uint8_t v[F25519_SIZE];
	uint8_t i[F25519_SIZE];
	uint8_t x[F25519_SIZE];
	uint8_t y[F25519_SIZE];

	/* v = (2a)^((p-5)/8) [x = 2a] */
	f25519_mul_c(x, a, 2);
	exp2523(v, x, y);

	/* i = 2av^2 - 1 */
	f25519_mul__distinct(y, v, v);
	f25519_mul__distinct(i, x, y);
	f25519_load(y, 1);
	f25519_sub(i, i, y);

	/* r = avi */
	f25519_mul__distinct(x, v, a);
	f25519_mul__distinct(r, x, i);
}


/* Convert an Edwards Y to a Montgomery X (Edwards X is not used).
 * Resulting coordinate is normalized.
 */




void morph25519_e2m(uint8_t *montgomery, const uint8_t *y)
{
	uint8_t yplus[F25519_SIZE];
	uint8_t yminus[F25519_SIZE];

	f25519_sub(yplus, f25519_one, y);
	// tohex(yplus);
	f25519_inv__distinct(yminus, yplus);
	// tohex(yminus);
	f25519_add(yplus, f25519_one, y);
	// tohex(yplus);
	f25519_mul__distinct(montgomery, yplus, yminus);
	// tohex(montgomery);
	f25519_normalize(montgomery);
	// tohex(montgomery);
}


int main (int argc, char *argv[]) {
	char* hexstr;
	uint8_t* inp;

	if (argc == 1) {
		printf("Enter Ed25519 public key hex string");
		exit(0);
	}
	hexstr = argv[1];
	inp = datahex(hexstr);
	uint8_t* out = malloc(32);
	morph25519_e2m(out, inp);
	tohex(out);
}

