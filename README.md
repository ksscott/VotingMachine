# <img alt="Logo" src="src/main/resources/images/listicon.png" height="30"/> Voting Machine

<p>
    <img alt="Voting Badge" src="src/main/resources/images/votingbadge.svg" />
    <a href="https://github.com/ksscott/VotingMachine/graphs/contributors">
        <img src="https://img.shields.io/github/contributors/ksscott/VotingMachine" alt="Contributors" /></a>
    <a href="https://github.com/ksscott/VotingMachine/pulse">
        <img src="https://img.shields.io/github/commit-activity/m/ksscott/VotingMachine" alt="Activity" /></a>
</p>

A voting machine capable of deciding an election by instant runoff.

## Getting Started

### Discord Bot

This application is primarily intended to be run as a Discord bot. 
Follow Discord's [instructions](https://discord.com/developers/docs/getting-started) for adding a bot to your server. 
When it's configured, execute this application from the main method in [Bot.java](src/discord/bot/Bot.java), 
    passing in your bot's [token](https://discord.com/developers/docs/getting-started#configuring-your-bot) 
    as the single runtime argument.
The application will connect to your server and register various 
    [slash commands](https://discord.com/blog/slash-commands-are-here) needed to interact with the bot. 
Type `/help` in a channel called `#bot-commands` to get started.

## Elections

### Candidates

The Discord bot initializes an election with a set of games to choose between. 
Tweak the source code to replace these with the candidates of your choosing. 
Additional candidates can be suggested after the election starts.

### Evaluation Algorithms

By default, a [Weighted Runoff](https://electowiki.org/wiki/Instant_Runoff_Normalized_Ratings) 
    process is used to decide elections. 
Several other algorithms have been included; pass one in to the [Evaluator](src/algorithm/Evaluator.java) 
    method below to evaluate the election using an alternate algorithm.

`evaluateElection(Election<V> election, Function<Race,EvalAlgorithm<V>> algorithm)`

### Races

Multiple "races" are supported, to allow simultaneous election of a winner in multiple categories, 
    similar to political elections. 
Currently, the Discord bot only supports elections with a single race.

## Dependencies
* [JDA](https://github.com/discord-jda/JDA)
* [JFreeChart](https://www.jfree.org/jfreechart/)
