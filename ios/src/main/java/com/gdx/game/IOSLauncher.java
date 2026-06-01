package com.gdx.game;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.gdx.game.game.DetectiveGame;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

/** Launches the iOS (RoboVM) application. */
public class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        String apiKey  = System.getenv("OPENAI_API_KEY");
        String groqKey = System.getenv("GROQ_API_KEY");

        IOSApplicationConfiguration configuration = new IOSApplicationConfiguration();
        return new IOSApplication(new DetectiveGame(apiKey, groqKey), configuration);
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
