package com.rinostick.app;

import android.service.notification.NotificationListenerService;

/**
 * Não faz nada sozinho: existir e estar habilitado (via adb) é o que dá ao
 * launcher acesso às sessões de mídia ativas (Spotify etc.) para o "Tocando agora".
 */
public class NowPlayingListener extends NotificationListenerService { }
