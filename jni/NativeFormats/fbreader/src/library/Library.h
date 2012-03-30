
#ifndef __LIBRARY_H__
#define __LIBRARY_H__

#include <jni.h>

#include <string>

#include <shared_ptr.h>


class Library {

public:
	static Library &Instance();

private:
	static shared_ptr<Library> ourInstance;

private:
	Library();

public:
	~Library();

	std::string cacheDirectory() const;

private:
	jclass myPathsClass;
};

#endif /* __LIBRARY_H__ */
