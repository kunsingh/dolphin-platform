/*
 * Copyright 2015-2017 Canoo Engineering AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.canoo.dolphin.client.javafx.stage;

import com.canoo.dolphin.client.javafx.view.AbstractViewBinder;
import com.canoo.impl.platform.core.Assert;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A JavaFX {@link Stage} that contains the view of a {@link AbstractViewBinder} and will automatically call
 * {@link AbstractViewBinder#destroy()} when the stage becomes hidden.
 * @param <M> type of the model
 */
public class DolphinStage<M> extends Stage {

    /**
     * Constructor
     * @param viewBinder the viewBinder
     */
    public DolphinStage(final AbstractViewBinder<M> viewBinder) {
        Assert.requireNonNull(viewBinder, "viewBinder");
        DolphinWindowUtils.destroyOnClose(this, viewBinder);
        setScene(new Scene(viewBinder.getParent()));
    }
}
