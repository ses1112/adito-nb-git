package de.adito.git.gui.window.content;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * @author m.kaspera 29.10.2018
 */
public class WindowContentModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(IWindowContentFactory.class));
        bind(IWindowContentProvider.class).to(WindowContentProviderImpl.class);
    }
}