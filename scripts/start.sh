#!/bin/bash

exec java -jar /home/bot/Run/pollster.jar $(cat /home/bot/Run/token.txt)
