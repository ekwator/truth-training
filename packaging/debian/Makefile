DEB_NAME=truth-core-server.deb
PACKAGE_ROOT=$(CURDIR)/build/truth-core-server

all: deb

deb:
	rm -rf $(PACKAGE_ROOT) && mkdir -p $(PACKAGE_ROOT)
	mkdir -p $(PACKAGE_ROOT)/usr/bin
	mkdir -p $(PACKAGE_ROOT)/etc/truth-core-server
	mkdir -p $(PACKAGE_ROOT)/usr/lib/truth-core-server
	mkdir -p $(PACKAGE_ROOT)/DEBIAN
	# Rename binary from truth_core_server to truth-core-server
	cp ../../target/release/truth_core_server $(PACKAGE_ROOT)/usr/bin/truth-core-server
	chmod 755 $(PACKAGE_ROOT)/usr/bin/truth-core-server
	cp config.yml $(PACKAGE_ROOT)/etc/truth-core-server/config.yml
	# Copy service file to temporary location for postinst to install to user directory
	cp truth-core-server.service $(PACKAGE_ROOT)/usr/lib/truth-core-server/truth-core-server.service
	cp control $(PACKAGE_ROOT)/DEBIAN/control
	cp postinst $(PACKAGE_ROOT)/DEBIAN/postinst
	cp postrm $(PACKAGE_ROOT)/DEBIAN/postrm
	cp prerm $(PACKAGE_ROOT)/DEBIAN/prerm
	chmod 755 $(PACKAGE_ROOT)/DEBIAN/postinst $(PACKAGE_ROOT)/DEBIAN/prerm $(PACKAGE_ROOT)/DEBIAN/postrm
	dpkg-deb --build --root-owner-group $(PACKAGE_ROOT) ../$(DEB_NAME)

clean:
	rm -rf build *.deb
