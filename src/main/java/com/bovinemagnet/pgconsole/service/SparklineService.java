package com.bovinemagnet.pgconsole.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class SparklineService {

    public String generateSparkline(List<Double> values, int width, int height) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double range = max - min;
        if (range == 0) range = 1.0;

        StringBuilder path = new StringBuilder("M 0 " + height);
        double step = (double) width / (values.size() - 1);
        
        for (int i = 0; i < values.size(); i++) {
            double x = i * step;
            double normalizedValue = (values.get(i) - min) / range;
            double y = height - (normalizedValue * height);
            
            if (i == 0) {
                path.append(" ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
            } else {
                path.append(" L ").append(String.format("%.2f", x)).append(" ").append(String.format("%.2f", y));
            }
        }

        return String.format(
            "<svg width=\"%d\" height=\"%d\" class=\"sparkline\" xmlns=\"http://www.w3.org/2000/svg\">" +
            "<path d=\"%s\" fill=\"none\" stroke=\"#0d6efd\" stroke-width=\"1.5\"/>" +
            "</svg>",
            width, height, path.toString()
        );
    }
}
