FROM eclipse-temurin:17 as build
WORKDIR /verinice

RUN apt-get update && apt-get install -y \
	unzip \
	&& rm -rf /var/lib/apt/lists/*

# The mkdir is not really necessary
# https://docs.docker.com/develop/develop-images/dockerfile_best-practices/#add-or-copy
RUN mkdir -p /verinice/sernet.verinice.extraresources.jre_linux_64 \
	&& mkdir -p /verinice/sernet.verinice.extraresources.feature/linux \
    && curl -SL https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jre_x64_linux_hotspot_11.0.20_8.tar.gz \
	|  tar -xzC /verinice/sernet.verinice.extraresources.jre_linux_64 \
	&& mv /verinice/sernet.verinice.extraresources.jre_linux_64/jdk-*-jre /verinice/sernet.verinice.extraresources.jre_linux_64/jre \
	&& cp -R /verinice/sernet.verinice.extraresources.jre_linux_64/jre /verinice/sernet.verinice.extraresources.feature/linux

RUN mkdir -p /verinice/sernet.verinice.extraresources.jre_macos_64 \
	&& mkdir -p /verinice/sernet.verinice.extraresources.feature/macos \
    && curl -SL https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jre_x64_mac_hotspot_11.0.20_8.tar.gz \
	|  tar -xzC /verinice/sernet.verinice.extraresources.jre_macos_64 \
	&& mv /verinice/sernet.verinice.extraresources.jre_macos_64/jdk-*-jre /verinice/sernet.verinice.extraresources.jre_macos_64/jre \
	&& cp -R /verinice/sernet.verinice.extraresources.jre_macos_64/jre /verinice/sernet.verinice.extraresources.feature/macos

RUN mkdir -p /verinice/sernet.verinice.extraresources.jre_windows_64 \
	&& mkdir -p /verinice/sernet.verinice.extraresources.feature/windows \
    && curl -SL https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.20%2B8/OpenJDK11U-jre_x64_windows_hotspot_11.0.20_8.zip > /verinice/jre_windows.zip \
	&& unzip /verinice/jre_windows.zip -d /verinice/sernet.verinice.extraresources.jre_windows_64 \
	&& rm /verinice/jre_windows.zip \
	&& mv /verinice/sernet.verinice.extraresources.jre_windows_64/jdk-*-jre /verinice/sernet.verinice.extraresources.jre_windows_64/jre \
	&& cp -R /verinice/sernet.verinice.extraresources.jre_windows_64/jre /verinice/sernet.verinice.extraresources.feature/windows

ENV JAVA_OPTS "-Djdk.util.zip.disableZip64ExtraFieldValidation"
ENV JAVA_TOOL_OPTIONS "-Djdk.util.zip.disableZip64ExtraFieldValidation"

COPY . /verinice

RUN ./mvnw -Djdk.util.zip.disableZip64ExtraFieldValidation -Dtycho.disableP2Mirrors=true clean verify

FROM scratch as verinice

ENV output=/verinice

COPY --from=build /verinice/sernet.verinice.releng.client.product/target/products/*.zip ${OUTPUT_DIRECTORY}/client/
COPY --from=build /verinice/sernet.verinice.releng.server.product/target/* ${OUTPUT_DIRECTORY}/server/