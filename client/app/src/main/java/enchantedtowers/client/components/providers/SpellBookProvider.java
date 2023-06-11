package enchantedtowers.client.components.providers;

import android.app.Activity;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import enchantedtowers.client.components.utils.ClientUtils;
import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.SpellBookResponse;
import enchantedtowers.common.utils.proto.services.SpellBookServiceGrpc;
import enchantedtowers.common.utils.storage.ServerApiStorage;
import enchantedtowers.game_logic.json.DefendSpellsTemplatesProvider;
import enchantedtowers.game_logic.json.SpellsTemplatesProvider;
import enchantedtowers.game_models.SpellBook;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class SpellBookProvider {
    private SpellBookServiceGrpc.SpellBookServiceStub asyncStub;
    private final Logger logger = Logger.getLogger(SpellBookProvider.class.getName());
    private static class Holder {
        static final SpellBookProvider INSTANCE = new SpellBookProvider();
    }

    public static SpellBookProvider getInstance() {
        return Holder.INSTANCE;
    }

    public void provideSpellBook(Activity context) {
        // initialize spell book
        if (!SpellBook.isInstantiated()) {
                String host = ServerApiStorage.getInstance().getClientHost();
                int port = ServerApiStorage.getInstance().getPort();
                ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                        .usePlaintext()
                        .build();
                asyncStub = SpellBookServiceGrpc.newStub(channel);
                asyncStub
                    .withDeadlineAfter(ServerApiStorage.getInstance().getClientRequestTimeout(), TimeUnit.MILLISECONDS)
                    .retrieveSpellBookAsJSON(Empty.newBuilder().build(), new StreamObserver<>() {
                        SpellBookResponse response;

                        @Override
                        public void onNext(SpellBookResponse response) {
                            if (response.hasError()) {
                                ClientUtils.showSnackbar(context.findViewById(android.R.id.content).getRootView(), response.getError().getMessage(), Snackbar.LENGTH_LONG);
                                logger.warning("retrieveSpellBookAsJSON::Received error=" + response.getError().getMessage());
                            }
                            else {
                                logger.info("retrieveSpellBookAsJSON::Received response=" + response.getJsonData());
                            }

                            this.response = response;
                        }

                        @Override
                        public void onError(Throwable t) {
                            ClientUtils.showSnackbar(context.findViewById(android.R.id.content).getRootView(), t.getMessage(), Snackbar.LENGTH_LONG);
                            SpellBook.instantiate(List.of(), List.of());
                            logger.warning("retrieveSpellBookAsJSON::onError error=" + t.getMessage());
                        }

                        @Override
                        public void onCompleted() {
                            List<SpellsTemplatesProvider.SpellTemplateData> spellTemplatesData = List.of();
                            List<DefendSpellsTemplatesProvider.DefendSpellTemplateData> defendSpellTemplatesData = List.of();

                            if (!response.hasError()) {
                                try {
                                    spellTemplatesData = SpellsTemplatesProvider.parseSpellsJson(
                                        response.getJsonData()
                                    );
                                    defendSpellTemplatesData = DefendSpellsTemplatesProvider.parseDefendSpellsJson(
                                            response.getJsonData()
                                    );
                                } catch (JSONException e) {
                                    spellTemplatesData = List.of();
                                    defendSpellTemplatesData = List.of();
                                    ClientUtils.showSnackbar(context.findViewById(android.R.id.content).getRootView(), e.getMessage(), Snackbar.LENGTH_LONG);
                                    logger.warning("retrieveSpellBookAsJSON::Received error: " + e.getMessage());
                                }
                            }

                            SpellBook.instantiate(spellTemplatesData, defendSpellTemplatesData);
                        }
                    });
            }
        }
}
