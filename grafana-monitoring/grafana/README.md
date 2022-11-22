# Grafana with docker

This image deploy (Grafana)[https://grafana.com] version 9.0.2.

## Configuration

All options defined in `./grafana.ini` can be overridden using environment
variables by using the syntax `GF_<SectionName>_<KeyName>`.

> For any changes to `./grafana.ini` (or corresponding environment variables) to take effect you need to restart Grafana by restarting the Docker container.

### Default Paths

The following settings are hard-coded when launching the Grafana Docker container and can only be overridden using environment variables, not in `conf/grafana.ini`.

Setting               | Default value
----------------------|---------------------------
GF_PATHS_CONFIG       | /etc/grafana/grafana.ini
GF_PATHS_DATA         | /var/lib/grafana
GF_PATHS_HOME         | /usr/share/grafana
GF_PATHS_LOGS         | /var/log/grafana
GF_PATHS_PLUGINS      | /var/lib/grafana/plugins
GF_PATHS_PROVISIONING | /etc/grafana/provisioning

## Installing Plugins for Grafana

Pass the plugins you want installed to docker with the `GF_INSTALL_PLUGINS` environment variable as a comma separated list. This will pass each plugin name to `grafana-cli plugins install ${plugin}` and install them when Grafana starts.

```bash
docker run \
  -d \
  -p 3000:3000 \
  --name=grafana \
  -e "GF_INSTALL_PLUGINS=grafana-clock-panel,grafana-simple-json-datasource" \
  grafana/grafana
```

## Installing Plugins from other sources

It's possible to install plugins from custom url:s by specifying the url like this: `GF_INSTALL_PLUGINS=<url to plugin zip>;<plugin name>`

```bash
docker run \
  -d \
  -p 3000:3000 \
  --name=grafana \
  -e "GF_INSTALL_PLUGINS=http://plugin-domain.com/my-custom-plugin.zip;custom-plugin" \
  grafana/grafana
```

## Grafana container with persistent storage (recommended)

```bash
# create a persistent volume for your data in /var/lib/grafana (database and plugins)
docker volume create grafana-storage

# start grafana
docker run \
  -d \
  -p 3000:3000 \
  --name=grafana \
  -v grafana-storage:/var/lib/grafana \
  grafana/grafana
```

## Grafana container using bind mounts

You may want to run Grafana in Docker but use folders on your host for the database or configuration. When doing so it becomes important to start the container with a user that is able to access and write to the folder you map into the container.

```bash
mkdir data # creates a folder for your data
ID=$(id -u) # saves your user id in the ID variable

# starts grafana with your user id and using the data folder
docker run -d --user $ID --volume "$PWD/data:/var/lib/grafana" -p 3000:3000 grafana/grafana:5.1.0
```

## Reading secrets from files (support for Docker Secrets)

It's possible to supply Grafana with configuration through files. This works well with [Docker Secrets](https://docs.docker.com/engine/swarm/secrets/) as the secrets by default gets mapped into `/run/secrets/<name of secret>` of the container.

You can do this with any of the configuration options in conf/grafana.ini by setting `GF_<SectionName>_<KeyName>__FILE` to the path of the file holding the secret.

Let's say you want to set the admin password this way.

- Admin password secret: `/run/secrets/admin_password`
- Environment variable: `GF_SECURITY_ADMIN_PASSWORD__FILE=/run/secrets/admin_password`

**Important changes**

* file ownership is no longer modified during startup with `chown`
* default user id `472` instead of `104`
* no more implicit volumes
  - `/var/lib/grafana`
  - `/etc/grafana`
  - `/var/log/grafana`

## Logging in for the first time

To run Grafana open your browser and go to http://localhost:3000/. 3000 is the default http port that Grafana listens to if you haven't [configured a different port](/installation/configuration/#http-port).
Then follow the instructions [here](/guides/getting_started/).
