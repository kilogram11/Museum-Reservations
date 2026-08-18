package com.museum.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "museum.ai.rag")
public class RagProperties {

    private boolean enabled = true;
    private int topK = 4;
    private float minScore = 0.15f;
    private boolean rebuildOnStartup = true;
    private Sources sources = new Sources();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public float getMinScore() {
        return minScore;
    }

    public void setMinScore(float minScore) {
        this.minScore = minScore;
    }

    public boolean isRebuildOnStartup() {
        return rebuildOnStartup;
    }

    public void setRebuildOnStartup(boolean rebuildOnStartup) {
        this.rebuildOnStartup = rebuildOnStartup;
    }

    public Sources getSources() {
        return sources;
    }

    public void setSources(Sources sources) {
        this.sources = sources;
    }

    public static class Sources {
        private boolean newsEnabled = true;
        private boolean staticRulesEnabled = true;

        public boolean isNewsEnabled() {
            return newsEnabled;
        }

        public void setNewsEnabled(boolean newsEnabled) {
            this.newsEnabled = newsEnabled;
        }

        public boolean isStaticRulesEnabled() {
            return staticRulesEnabled;
        }

        public void setStaticRulesEnabled(boolean staticRulesEnabled) {
            this.staticRulesEnabled = staticRulesEnabled;
        }
    }
}
