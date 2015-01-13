package org.msf.records.data.res;

import android.content.res.Resources;

/**
 * An interface that can be resolved using Android {@link Resources}.
 */
interface Resolvable<T> {

    T resolve(Resources resources);
}
