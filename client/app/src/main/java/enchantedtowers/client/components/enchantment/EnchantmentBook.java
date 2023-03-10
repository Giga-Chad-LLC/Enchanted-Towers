package enchantedtowers.client.components.enchantment;

import android.graphics.Path;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Arrays;

public class EnchantmentBook {
    static ArrayList <PointF> circle = new ArrayList<>(Arrays.asList(
            new PointF(42.978516f, 85.97351f), new PointF(33.248505f, 85.97351f), new PointF(28.283112f, 85.97351f), new PointF(23.356628f, 85.97351f), new PointF(15.797333f, 92.66992f), new PointF(14.996338f, 99.0152f), new PointF(9.014282f, 114.461975f), new PointF(7.198181f, 116.05072f), new PointF(1.9775391f, 130.01038f), new PointF(1.5466919f, 140.28552f), new PointF(0.0f, 189.02222f), new PointF(0.0f, 203.89343f), new PointF(0.0f, 228.1872f), new PointF(0.0f, 250.06671f), new PointF(0.0f, 265.59222f), new PointF(0.0f, 274.96356f), new PointF(0.0f, 285.88806f), new PointF(7.445801f, 300.02197f), new PointF(16.037262f, 313.0276f), new PointF(27.0112f, 331.00568f), new PointF(37.272522f, 339.9646f), new PointF(52.700745f, 356.51202f), new PointF(59.7222f, 368.96118f), new PointF(76.477295f, 378.38788f), new PointF(87.3681f, 385.48938f), new PointF(100.34247f, 397.3158f), new PointF(118.05527f, 399.29346f), new PointF(131.27203f, 405.00366f), new PointF(157.52252f, 407.98462f), new PointF(168.7804f, 407.98462f), new PointF(187.50525f, 407.98462f), new PointF(200.19226f, 407.98462f), new PointF(211.77948f, 407.98462f), new PointF(232.44855f, 407.98462f), new PointF(247.5055f, 399.0078f), new PointF(262.16858f, 397.95776f), new PointF(279.8449f, 387.1239f), new PointF(294.3667f, 382.98523f), new PointF(305.81104f, 375.16364f), new PointF(323.84576f, 368.96118f), new PointF(339.33954f, 356.4461f), new PointF(348.27563f, 347.4383f), new PointF(362.78436f, 339.17346f), new PointF(376.6151f, 320.28973f), new PointF(386.97144f, 297.0291f), new PointF(390.99243f, 283.07397f), new PointF(393.9917f, 258.4964f), new PointF(397.97974f, 242.7442f), new PointF(397.97974f, 229.49982f), new PointF(397.97974f, 210.64886f), new PointF(397.97974f, 193.27649f), new PointF(396.88257f, 175.97339f), new PointF(387.81647f, 152.47906f), new PointF(380.836f, 133.8421f), new PointF(368.0547f, 112.44025f), new PointF(353.93414f, 94.61066f), new PointF(339.79053f, 80.59979f), new PointF(323.16168f, 61.1391f), new PointF(311.89862f, 49.87732f), new PointF(299.30322f, 39.01184f), new PointF(283.46375f, 25.507507f), new PointF(281.5077f, 23.517883f), new PointF(272.99927f, 17.509338f), new PointF(256.96857f, 10.975342f), new PointF(248.62885f, 10.975342f), new PointF(233.7276f, 6.9781494f), new PointF(226.107f, 0.10748291f), new PointF(214.12146f, 0.0f), new PointF(197.49023f, 0.0f), new PointF(183.46619f, 0.0f), new PointF(168.8164f, 0.0f), new PointF(157.38324f, 0.0f), new PointF(140.50415f, 0.0f), new PointF(136.72003f, 6.4315796f), new PointF(122.91931f, 10.441223f), new PointF(114.341156f, 10.975342f), new PointF(104.51294f, 10.975342f), new PointF(96.99829f, 16.615234f), new PointF(86.88931f, 22.018433f), new PointF(83.34488f, 24.6474f), new PointF(78.27368f, 25.970947f), new PointF(72.15982f, 28.996582f), new PointF(69.971924f, 28.996582f), new PointF(65.867584f, 38.809204f), new PointF(53.11386f, 43.39972f), new PointF(45.172546f, 51.36438f), new PointF(42.978516f, 55.994568f), new PointF(39.01709f, 53.99597f), new PointF(27.514832f, 65.82318f), new PointF(29.003906f, 68.736084f), new PointF(24.98291f, 68.02002f), new PointF(20.194153f, 76.79175f), new PointF(13.496704f, 84.99115f), new PointF(14.996338f, 86.639404f), new PointF(14.996338f, 85.97351f)
    ));

    static ArrayList <PointF> angleRight = new ArrayList<>(Arrays.asList(
            new PointF(0.0f, 0.0f), new PointF(4.020996f, 0.0f), new PointF(28.474884f, 0.0f), new PointF(39.476532f, 0.0f), new PointF(48.99353f, 0.0f), new PointF(60.00183f, 5.9957886f), new PointF(61.00708f, 3.9971924f), new PointF(71.89917f, 6.891968f), new PointF(91.529144f, 6.9781494f), new PointF(103.529205f, 10.323181f), new PointF(115.25635f, 10.975342f), new PointF(121.89383f, 12.203552f), new PointF(130.30777f, 13.956299f), new PointF(135.40604f, 13.956299f), new PointF(145.41364f, 13.956299f), new PointF(155.78622f, 13.956299f), new PointF(163.46689f, 13.956299f), new PointF(171.45529f, 13.956299f), new PointF(175.6901f, 13.956299f), new PointF(190.00854f, 14.213135f), new PointF(194.80847f, 17.953491f), new PointF(198.08258f, 21.950684f), new PointF(207.49896f, 21.950684f), new PointF(216.77258f, 21.950684f), new PointF(230.4245f, 21.950684f), new PointF(241.09961f, 21.950684f), new PointF(254.63153f, 21.950684f), new PointF(259.50256f, 21.950684f), new PointF(271.82568f, 21.950684f), new PointF(276.0583f, 21.950684f), new PointF(281.9806f, 21.950684f), new PointF(289.01733f, 21.950684f), new PointF(300.271f, 21.950684f), new PointF(314.77228f, 21.950684f), new PointF(332.68372f, 21.950684f), new PointF(334.48425f, 21.950684f), new PointF(344.68317f, 21.950684f), new PointF(345.49255f, 21.950684f), new PointF(350.68213f, 21.950684f), new PointF(355.36957f, 21.950684f), new PointF(358.2589f, 21.950684f), new PointF(358.0005f, 28.683228f), new PointF(358.0005f, 35.909485f), new PointF(358.0005f, 48.345703f), new PointF(358.0005f, 52.91565f), new PointF(360.9214f, 59.90454f), new PointF(361.98853f, 66.96991f), new PointF(361.98853f, 73.94806f), new PointF(366.00952f, 81.8634f), new PointF(366.00952f, 91.08551f), new PointF(366.00952f, 98.86035f), new PointF(366.00952f, 107.239746f), new PointF(366.00952f, 111.21704f), new PointF(366.00952f, 123.94684f), new PointF(366.00952f, 134.49335f), new PointF(366.00952f, 142.42462f), new PointF(366.00952f, 153.54285f), new PointF(369.0088f, 166.65698f), new PointF(369.0088f, 175.03088f), new PointF(369.0088f, 190.32123f), new PointF(369.0088f, 200.32562f), new PointF(374.99084f, 211.51245f), new PointF(372.99683f, 222.89893f), new PointF(372.99683f, 229.42993f), new PointF(372.99683f, 238.19836f), new PointF(372.99683f, 250.30615f), new PointF(372.99683f, 261.8155f), new PointF(372.99683f, 273.4676f), new PointF(372.99683f, 282.98767f), new PointF(372.99683f, 292.65656f), new PointF(372.99683f, 303.92822f), new PointF(375.9961f, 313.35126f), new PointF(375.9961f, 320.65088f), new PointF(375.9961f, 328.7514f), new PointF(375.9961f, 340.5066f), new PointF(375.9961f, 344.1894f), new PointF(375.9961f, 361.5088f), new PointF(375.9961f, 372.4502f), new PointF(375.9961f, 382.3694f), new PointF(375.9961f, 394.54224f), new PointF(375.9961f, 397.95776f), new PointF(375.9961f, 401.59436f), new PointF(375.9961f, 400.93872f)
    ));

    static ArrayList <PointF> angleLeft = new ArrayList<>(Arrays.asList(
            new PointF(75.01465f, 0.0f), new PointF(75.01465f, 8.635986f), new PointF(75.01465f, 12.289368f), new PointF(75.01465f, 20.352905f), new PointF(80.3457f, 58.31201f), new PointF(83.02368f, 75.970764f), new PointF(83.02368f, 93.83954f), new PointF(83.02368f, 104.16016f), new PointF(83.02368f, 114.99402f), new PointF(86.02295f, 121.90234f), new PointF(90.01099f, 130.46558f), new PointF(89.806305f, 147.66736f), new PointF(86.02295f, 167.36816f), new PointF(83.02368f, 195.89252f), new PointF(83.02368f, 218.6593f), new PointF(83.02368f, 231.80145f), new PointF(83.02368f, 249.70203f), new PointF(83.02368f, 265.71985f), new PointF(75.01465f, 279.44843f), new PointF(75.01465f, 293.9662f), new PointF(70.51575f, 302.9397f), new PointF(65.98389f, 309.9856f), new PointF(61.00708f, 313.49683f), new PointF(61.00708f, 316.96387f), new PointF(57.019043f, 314.9652f), new PointF(49.614075f, 314.9652f), new PointF(45.499878f, 314.9652f), new PointF(32.951508f, 305.63562f), new PointF(24.39038f, 296.31702f), new PointF(17.995605f, 286.9596f), new PointF(3.5266113f, 272.41882f), new PointF(0.29507446f, 265.47852f), new PointF(0.0f, 260.85883f), new PointF(0.0f, 257.98822f), new PointF(6.0929565f, 257.98828f), new PointF(16.028534f, 253.99109f), new PointF(30.879944f, 253.99109f), new PointF(36.457855f, 253.99109f), new PointF(46.505127f, 253.99109f), new PointF(55.087494f, 253.99109f), new PointF(67.62195f, 257.98828f), new PointF(80.13562f, 261.8161f), new PointF(87.52258f, 266.96503f), new PointF(96.000854f, 267.9474f), new PointF(99.99756f, 276.36603f), new PointF(110.00061f, 275.94177f), new PointF(116.987915f, 287.4591f), new PointF(122.01041f, 285.96863f), new PointF(129.49142f, 289.96582f), new PointF(133.02246f, 294.96545f), new PointF(142.46057f, 296.94397f), new PointF(147.03683f, 300.94116f), new PointF(158.00537f, 303.76672f), new PointF(165.1507f, 303.98987f), new PointF(180.384f, 303.98987f), new PointF(193.5022f, 303.98987f), new PointF(203.78412f, 307.98706f), new PointF(216.49359f, 307.98706f), new PointF(223.29956f, 307.98706f), new PointF(232.34491f, 310.96802f), new PointF(240.7633f, 310.96802f), new PointF(248.09772f, 310.96802f), new PointF(258.00293f, 313.3368f), new PointF(264.59705f, 314.9652f), new PointF(272.67963f, 314.9652f), new PointF(277.99255f, 314.9652f), new PointF(288.25006f, 317.96252f), new PointF(293.40082f, 320.89526f), new PointF(293.99414f, 318.9624f), new PointF(303.0249f, 318.9624f), new PointF(308.3299f, 321.94336f), new PointF(313.98376f, 321.94336f), new PointF(314.989f, 321.94336f), new PointF(319.01f, 321.94336f), new PointF(319.01f, 321.94336f)
    ));


    static private EnchantmentBook book = new EnchantmentBook();
    public ArrayList<Enchantment> templates;

    static public EnchantmentBook getInstance() {
        return book;
    }

    private EnchantmentBook() {
//        templates = new ArrayList<>(Arrays.asList(
//            new Enchantment(createPathFromPoints(circle), Color.CYAN, circle, new PointF(0f, 0f)),
//            new Enchantment(createPathFromPoints(angleLeft), Color.CYAN, angleLeft, new PointF(0f, 0f)),
//            new Enchantment(createPathFromPoints(angleRight), Color.CYAN, angleRight, new PointF(0f, 0f))
//        ));
        templates = new ArrayList<>();
    }

    private Path createPathFromPoints(ArrayList<PointF> points) {
        Path p = new Path();

        if (points.isEmpty()) {
            return p;
        }

        p.moveTo(points.get(0).x, points.get(0).y);

        for (int i = 1; i < points.size(); i++) {
            PointF pt = points.get(i);
            p.lineTo(pt.x, pt.y);
        }

        return p;
    }

    public static Enchantment getMatchedTemplate(Enchantment pattern) {
        return null;
    }

    private float getHausdorffDistance(Enchantment A, Enchantment B) {
        return 0f;
    }
}