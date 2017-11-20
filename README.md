# boxpile

A means of defining an app in a config file, and running it in docker containers locally.

Builds containers via Chef, and runs linked groups of containers locally.

## Build Status: [![CircleCI](https://circleci.com/gh/nikogura/boxpile.svg?style=svg)](https://circleci.com/gh/nikogura/boxpile)

## Goal

Take an app running in prod, and model it locally up to the limits of the hardware.

## Status

This is a rewrite of some tools I've written a couple times now.  The basic architecture is sound and works, but I have to finish working out the kinks and tests.  

It's really just 'Docker Compose' written in Java, and hooked to Chef.  It was written before docker compose was stable.

Now that docker compose exists, I'd ust that rather than this.
