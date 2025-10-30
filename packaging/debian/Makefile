DEB_NAME=truth-core-service.deb
PACKAGE_ROOT=$(CURDIR)/build/truth-core-service

all: deb

deb:
	rm -rf $(PACKAGE_ROOT) && mkdir -p $(PACKAGE_ROOT)
	mkdir -p $(PACKAGE_ROOT)/usr/local/bin
	mkdir -p $(PACKAGE_ROOT)/etc/truth-core
	mkdir -p $(PACKAGE_ROOT)/etc/systemd/system
	mkdir -p $(PACKAGE_ROOT)/DEBIAN
	cp ../../target/release/truth_core_server $(PACKAGE_ROOT)/usr/local/bin/truth_core
	cp config.yml $(PACKAGE_ROOT)/etc/truth-core/config.yml
	cp truth-core.service $(PACKAGE_ROOT)/etc/systemd/system/truth-core.service
	cp control $(PACKAGE_ROOT)/DEBIAN/control
	cp postinst $(PACKAGE_ROOT)/DEBIAN/postinst
	cp postrm $(PACKAGE_ROOT)/DEBIAN/postrm
	cp prerm $(PACKAGE_ROOT)/DEBIAN/prerm
	chmod 755 $(PACKAGE_ROOT)/DEBIAN/postinst $(PACKAGE_ROOT)/DEBIAN/prerm $(PACKAGE_ROOT)/DEBIAN/postrm
	dpkg-deb --build --root-owner-group $(PACKAGE_ROOT) ../$(DEB_NAME)

clean:
	rm -rf build *.deb
