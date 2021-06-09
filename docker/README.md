# Netspeak indexing Docker

This is the Docker image published at [webis/netspeak-indexing](https://hub.docker.com/r/webis/netspeak-indexing/tags?page=1&ordering=last_updated).

The Dockerfile will build Netspeak from a specific commit in the [GitHub repository](https://github.com/netspeak/netspeak4-indexing) of this project.

## Pulling

The docker image is publicly available for everyone!

```bash
docker run webis/netspeak-indexing:1.0.0 netspeak4-indexing --help
```

## Publishing

To publish the Docker image, you have to member of the [webis organization](https://hub.docker.com/u/webis). If you are a member, you can simply run the `publish-to-dockerhub.sh` script (don't forget to update the version number in the script).
