FROM eclipse-temurin:11 as build
RUN mkdir /verinice
WORKDIR /verinice

RUN apt-get update && apt-get install -y \
	unzip \
	&& rm -rf /var/lib/apt/lists/*

# https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#add-or-copy
RUN curl -SL https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jre_x64_linux_hotspot_11.0.20_8.tar.gz \
	| tar -xJC /verinice/sernet.verinice.extraresources.jre_linux_64 \
	&& mv /verinice/sernet.verinice.extraresources.jre_linux_64/jdk-*-jre /verinice/sernet.verinice.extraresources.jre_linux_64/jre \
	&& cp -R /verinice/sernet.verinice.extraresources.jre_linux_64/jre /verinice/sernet.verinice.extraresources.feature/linux

# Note: We're downloading aarch64 builds for macOS
RUN curl -SL https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jre_aarch64_mac_hotspot_11.0.20_8.tar.gz \
	| tar -xJC /verinice/sernet.verinice.extraresources.jre_macos_64 \
	&& mv /verinice/sernet.verinice.extraresources.jre_macos_64/jdk-*-jre /verinice/sernet.verinice.extraresources.jre_macos_64/jre \
	&& cp -R /verinice/sernet.verinice.extraresources.jre_macos_64/jre /verinice/sernet.verinice.extraresources.feature/macos

RUN curl -SL https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jre_x64_windows_hotspot_11.0.20_8.zip > /verinice/jre_windows.zip \
	&& unzip /verinice/jre_windows.zip -d /verinice/sernet.verinice.extraresources.jre_windows_64 \
	&& rm /verinice/jre_windows.zip
	&& mv /verinice/sernet.verinice.extraresources.jre_windows_64/jdk-*-jre /verinice/sernet.verinice.extraresources.jre_windows_64/jre \
	&& cp -R /verinice/sernet.verinice.extraresources.jre_windows_64/jre /verinice/sernet.verinice.extraresources.feature/windows

COPY . /verinice

RUN ./mvnw -Djdk.util.zip.disableZip64ExtraFieldValidation -Dtycho.disableP2Mirrors=true  clean verify

FROM debian:12 as verinice
RUN mkdir /verinice && mkdir /verinice/client && mkdir /verinice/server
COPY --from=build /verinice/sernet.verinice.releng.client.product/target/products/*.zip /verinice/client/
COPY --from=build /verinice/sernet.verinice.releng.server.product/target*.war /verinice/server/