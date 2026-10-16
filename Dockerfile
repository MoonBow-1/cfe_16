FROM rockylinux/rockylinux:9
WORKDIR /opt/teragrep/cfe_16
RUN --mount=type=bind,source=./rpm/,target=/rpm/ dnf --assumeyes upgrade && dnf --assumeyes install /rpm/*.rpm && dnf clean all;

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
USER srv-cfe_16
ENTRYPOINT /entrypoint.sh
