package com.github.tartakynov.cartoshka.functions;

import com.github.tartakynov.cartoshka.Function;
import com.github.tartakynov.cartoshka.tree.entities.Literal;
import com.github.tartakynov.cartoshka.tree.entities.literals.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class Functions {
    public static final Function hsla = new BaseFunction("hsla", 4) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Double h = Arguments.numeric(args, "h", getName(), false);
            Double s = Arguments.numeric(args, "s", getName(), false);
            Double l = Arguments.numeric(args, "l", getName(), false);
            Double a = Arguments.numeric(args, "a", getName(), false);
            return Color.fromHSLA(h.intValue(), s, l, a);
        }
    };

    public static final Function hsl = new BaseFunction("hsl", 3) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Double h = Arguments.numeric(args, "h", getName(), false);
            Double s = Arguments.numeric(args, "s", getName(), false);
            Double l = Arguments.numeric(args, "l", getName(), false);
            return Color.fromHSLA(h.intValue(), s, l, 1.0);
        }
    };

    public static final Function rgba = new BaseFunction("rgba", 4) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Double r = Arguments.numeric(args, "r", getName(), true);
            Double g = Arguments.numeric(args, "g", getName(), true);
            Double b = Arguments.numeric(args, "b", getName(), true);
            Double a = Arguments.numeric(args, "a", getName(), false);
            return Color.fromRGBA(r.intValue(), g.intValue(), b.intValue(), a);
        }
    };

    public static final Function rgb = new BaseFunction("rgb", 3) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Double r = Arguments.numeric(args, "r", getName(), true);
            Double g = Arguments.numeric(args, "g", getName(), true);
            Double b = Arguments.numeric(args, "b", getName(), true);
            return Color.fromRGBA(r.intValue(), g.intValue(), b.intValue(), 1.0);
        }
    };

    public static final Function hue = new BaseFunction("hue", 1) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            return new Numeric(color.getHue(), false);
        }
    };

    public static final Function saturation = new BaseFunction("saturation", 1) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            return new Dimension(color.getSaturation() * 100, "%");
        }
    };

    public static final Function lightness = new BaseFunction("lightness", 1) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            return new Dimension(color.getLightness() * 100, "%");
        }
    };

    public static final Function alpha = new BaseFunction("alpha", 1) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            return new Dimension(color.getAlpha() * 100, "%");
        }
    };

    public static final Function saturate = new BaseFunction("saturate", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            Double amount = Arguments.numeric(args, "amount", getName(), false);
            return Color.fromHSLA(
                    color.getHue(),
                    color.getSaturation() + amount,
                    color.getLightness(),
                    color.getAlpha()
            );
        }
    };

    public static final Function desaturate = new BaseFunction("desaturate", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            Double amount = Arguments.numeric(args, "amount", getName(), false);
            return Color.fromHSLA(
                    color.getHue(),
                    color.getSaturation() - amount,
                    color.getLightness(),
                    color.getAlpha()
            );
        }
    };

    public static final Function lighten = new BaseFunction("lighten", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            Double amount = Arguments.numeric(args, "amount", getName(), false);
            return Color.fromHSLA(
                    color.getHue(),
                    color.getSaturation(),
                    color.getLightness() + amount,
                    color.getAlpha()
            );
        }
    };

    public static final Function darken = new BaseFunction("darken", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            Double amount = Arguments.numeric(args, "amount", getName(), false);
            return Color.fromHSLA(
                    color.getHue(),
                    color.getSaturation(),
                    color.getLightness() - amount,
                    color.getAlpha()
            );
        }
    };

    public static final Function fadein = new BaseFunction("fadein", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            Double amount = Arguments.numeric(args, "amount", getName(), false);
            return Color.fromHSLA(
                    color.getHue(),
                    color.getSaturation(),
                    color.getLightness(),
                    color.getAlpha() + amount
            );
        }
    };

    public static final Function fadeout = new BaseFunction("fadeout", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            Double amount = Arguments.numeric(args, "amount", getName(), false);
            return Color.fromHSLA(
                    color.getHue(),
                    color.getSaturation(),
                    color.getLightness(),
                    color.getAlpha() - amount
            );
        }
    };

    public static final Function spin = new BaseFunction("spin", 2) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            int amount = Arguments.numeric(args, "amount", getName(), false).intValue();
            int hue = (color.getHue() + amount) % 360;
            return Color.fromHSLA(
                    hue < 0 ? 360 + hue : hue,
                    color.getSaturation(),
                    color.getLightness(),
                    color.getAlpha()
            );
        }
    };

    public static final Function greyscale = new BaseFunction("greyscale", 1) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color = Arguments.color(args, "color", getName());
            return Color.fromHSLA(
                    color.getHue(),
                    0,
                    color.getLightness(),
                    color.getAlpha()
            );
        }
    };

    public static final Function mix = new BaseFunction("mix", 3) {
        @Override
        public Literal apply(Iterator<Literal> args) {
            Color color1 = Arguments.color(args, "color1", getName());
            Color color2 = Arguments.color(args, "color2", getName());
            double p = Arguments.percent(args, "weight", getName());
            double w = p * 2d - 1d;
            double a = color1.getAlpha() - color2.getAlpha();
            double w1 = (((w * a == -1) ? w : (w + a) / (1 + w * a)) + 1) / 2.0;
            double w2 = 1d - w1;
            int r = (int) Math.round(color1.getRed() * w1 + color2.getRed() * w2);
            int g = (int) Math.round(color1.getGreen() * w1 + color2.getGreen() * w2);
            int b = (int) Math.round(color1.getBlue() * w1 + color2.getBlue() * w2);
            double alpha = color1.getAlpha() * p + color2.getAlpha() * (1 - p);
            return Color.fromRGBA(r, g, b, alpha);
        }
    };

    public static final Collection<Function> BUILTIN_FUNCTIONS = new ArrayList<Function>() {{
        add(Functions.alpha);
        add(Functions.darken);
        add(Functions.desaturate);
        add(Functions.hsl);
        add(Functions.hsla);
        add(Functions.hue);
        add(Functions.lighten);
        add(Functions.lightness);
        add(Functions.rgb);
        add(Functions.rgba);
        add(Functions.saturate);
        add(Functions.saturation);
        add(Functions.fadein);
        add(Functions.fadeout);
        add(Functions.spin);
        add(Functions.greyscale);
        add(Functions.mix);
    }};

    static {
        HashMap<String, Integer> imageFilterFunctions = new HashMap<String, Integer>() {{
            put("emboss", 0);
            put("blur", 0);
            put("gray", 0);
            put("sobel", 0);
            put("edge-detect", 0);
            put("x-gradient", 0);
            put("y-gradient", 0);
            put("sharpen", 0);
            put("agg-stack-blur", 2);
            put("scale-hsla", 8);
        }};

        for (java.util.Map.Entry<String, Integer> entry : imageFilterFunctions.entrySet()) {
            final String name = entry.getKey();
            final int argc = entry.getValue();
            BUILTIN_FUNCTIONS.add(new BaseFunction(name, argc) {
                @Override
                public Literal apply(Iterator<Literal> args) {
                    ArrayList<Literal> arguments = new ArrayList<>();
                    for (int i = 0; i < argc; i++) {
                        arguments.add(args.next());
                    }

                    return new ImageFilter(getName(), new MultiLiteral(arguments));
                }
            });
        }
    }
}