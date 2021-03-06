SUMMARY = "XBMC Media Center"

LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://LICENSE.GPL;md5=6eb631b6da7fdb01508a80213ffc35ff"

#Undeclared compile time dependencie, used from host system: openjdk-7, cmake, … 
DEPENDS = "libusb1 libcec libplist expat yajl gperf-native libxmu fribidi mpeg2dec ffmpeg samba fontconfig curl python libass libmodplug libmicrohttpd wavpack libmms cmake-native libsdl-image libsdl-mixer virtual/egl mysql5 sqlite3 libmms faad2 libcdio libpcre boost lzo enca avahi libsamplerate0 libxinerama libxrandr libxtst bzip2 virtual/libsdl jasper zip-native zlib zlib-native libtinyxml libmad swig-native curl-native python-json fakeroot libjpeg-turbo-native libpng-native"
#require recipes/egl/egl.inc


SRCREV = "9efd3582d0270c3f09cc2b0d44025070abd041a1"

PV = "15.1+gitr${SRCPV}"
PR = "r14"
SRC_URI = "git://github.com/xbmc/xbmc.git;branch=Isengard \
"

inherit autotools-brokensep gettext python-dir

S = "${WORKDIR}/git"

# breaks compilation
CCACHE = ""

CACHED_CONFIGUREVARS += " \
    ac_cv_path_PYTHON="${STAGING_BINDIR_NATIVE}/python-native/python" \
"

PACKAGECONFIG ??= "${@base_contains('DISTRO_FEATURES', 'opengl', 'opengl', 'openglesv2', d)}"
PACKAGECONFIG[opengl] = "--enable-gl,--enable-gles,glew"
PACKAGECONFIG[openglesv2] = "--enable-gles,--enable-gl,"

EXTRA_OECONF = " --disable-debug \
    --disable-rpath \
    --enable-libusb \
    --enable-airplay \
    --disable-optical-drive \
    --enable-external-libraries \
    ${@base_contains('DISTRO_FEATURES', 'opengl', '--enable-gl', '--enable-gles', d)} \
"

CXXFLAGS += " -I${STAGING_KERNEL_DIR}/include/uapi -I${STAGING_KERNEL_DIR}/include "
CFLAGS += " -I${STAGING_KERNEL_DIR}/include/uapi -I${STAGING_KERNEL_DIR}/include "

FULL_OPTIMIZATION_armv7a = "-fexpensive-optimizations -fomit-frame-pointer -O4 -ffast-math"
BUILD_OPTIMIZATION = "${FULL_OPTIMIZATION}"

EXTRA_OECONF_append_armv7a = "--cpu=cortex-a8"

# for python modules
export HOST_SYS
export BUILD_SYS
export STAGING_LIBDIR
export STAGING_INCDIR
export PYTHON_DIR

do_configure() {
    export TINYXML_CFLAGS="-I/${STAGING_INCDIR}"
    export TINYXML_LIBS="-L${STAGING_LIBDIR} -ltinyxml"
    export SQUISH_CFLAGS="-I/${STAGING_INCDIR}"
    export SQUISH_LIBS="-L${STAGING_LIBDIR} -lsquish"
    export PYTHON_EXTRA_LDFLAGS=""
    export PYTHON_EXTRA_LIBS="-lz"
    export PYTHON_VERSION="${PYTHON_BASEVERSION}"
    export PYTHON_NOVERSIONCHECK="no-check"
    export PYTHON_CPPFLAGS="-I/${STAGING_INCDIR}/${PYTHON_DIR}"
    export PYTHON_LDFLAGS="-L${STAGING_LIBDIR} -lpython${PYTHON_BASEVERSION}"

    sh bootstrap

    # I suspect this is a native tool, so cheat
    (
    unset CC
    unset LD
    unset LDFLAGS
    unset CFLAGS
    CFLAGS="$CFLAGS -I${STAGING_DIR_NATIVE}/usr/include/"
    CXXFLAGS="$CXXFLAGS -I${STAGING_DIR_NATIVE}/usr/include/"
    CPPFLAGS="$CPPFLAGS -I${STAGING_DIR_NATIVE}/usr/include/"

    cd tools/depends/native/JsonSchemaBuilder
#    sh ./autogen.sh
#    sh ./configure \
#    --prefix=${STAGING_DIR_NATIVE}/${prefix_native} 
#    --build=${BUILD_SYS} 
#    --host=${HOST_SYS} 
#    --target=${TARGET_SYS}

    make )

    oe_runconf

}

PARALLEL_MAKE = ""

do_compile_prepend() {
    for i in $(find . -name "Makefile") ; do
        sed -i -e 's:I/usr/include:I${STAGING_INCDIR}:g' $i
    done

    for i in $(find . -name "*.mak*" -o    -name "Makefile") ; do
        sed -i -e 's:I/usr/include:I${STAGING_INCDIR}:g' -e 's:-rpath \$(libdir):-rpath ${libdir}:g' $i
    done
}

INSANE_SKIP_${PN} = "rpaths"

# on ARM architectures xbmc will use GLES which will make the regular wrapper fail, so start it directly
do_install_append_arm() {
    sed -i -e 's:Exec=xbmc:Exec=${libdir}/xbmc/xbmc.bin:g' ${D}${datadir}/applications/xbmc.desktop
}

FILES_${PN} += "${datadir}/xsessions ${datadir}/icons"
FILES_${PN}-dbg += "${libdir}/xbmc/.debug ${libdir}/xbmc/*/.debug ${libdir}/xbmc/*/*/.debug ${libdir}/xbmc/*/*/*/.debug"

# xbmc uses some kind of dlopen() method for libcec so we need to add it manually
# OpenGL builds need glxinfo, that's in mesa-demos
RRECOMMENDS_${PN}_append = " libcec \
                             python \
                             python-lang \
                             python-re \
                             python-netclient \
                             libcurl \
                             xdpyinfo \
                             ${@base_contains('DISTRO_FEATURES', 'opengl', 'mesa-demos', '', d)} \
"
RRECOMMENDS_${PN}_append_libc-glibc = " glibc-charmap-ibm850 glibc-gconv-ibm850"
