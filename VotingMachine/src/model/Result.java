package model;

import org.jfree.data.flow.DefaultFlowDataset;

import java.util.HashSet;
import java.util.Set;

public class Result {
    private Set<Option> winners;
    private DefaultFlowDataset<String> data;

    public Result(Set<Option> winners, DefaultFlowDataset<String> data) {
        this.winners = winners;
        this.data = data;
    }

    public Set<Option> getWinners() { return new HashSet<>(winners); }

    public DefaultFlowDataset<String> getData() { return data; }
}
