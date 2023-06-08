package services;

import components.fs.FileReader;
import components.fs.ResourceProvider;
import enchantedtowers.common.utils.proto.common.Empty;
import enchantedtowers.common.utils.proto.responses.ServerError;
import enchantedtowers.common.utils.proto.responses.ServerError.ErrorType;
import enchantedtowers.common.utils.proto.responses.SpellBookResponse;
import enchantedtowers.common.utils.proto.services.SpellBookServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

public class SpellBookService extends SpellBookServiceGrpc.SpellBookServiceImplBase {
   private String cachedSpellBookJSON = null;
   private static final Logger logger = Logger.getLogger(FileReader.class.getName());

   @Override
   public void retrieveSpellBookAsJSON(Empty request, StreamObserver<SpellBookResponse> streamObserver) {
      if (cachedSpellBookJSON == null) {
         try {
            URL url = SpellBookService.class.getClassLoader().getResource(ResourceProvider.spellBookJSONFilename);
            if (url == null) {
               throw new IOException("URL: '" + ResourceProvider.spellBookJSONFilename + "' not found");
            }
            cachedSpellBookJSON = FileReader.readRawFile(url);
         }
         catch (IOException e) {
            logger.warning("Could not send file to user '" + ResourceProvider.spellBookJSONFilename + "', error=" + e.getMessage() + "stack=" + Arrays.toString(e.getStackTrace()));

            ServerError serverError = ServerError.newBuilder()
               .setType(ErrorType.SPELL_BOOK_NOT_LOADED)
               .setMessage("Unable to load spell book, try again")
               .build();

            SpellBookResponse response = SpellBookResponse.newBuilder()
               .setError(serverError)
               .setJsonData("")
               .build();


            streamObserver.onNext(response);
            streamObserver.onCompleted();
            return;
         }
      }

      SpellBookResponse response = SpellBookResponse.newBuilder()
          .setJsonData(cachedSpellBookJSON)
          .build();
      streamObserver.onNext(response);
      streamObserver.onCompleted();
   }
}