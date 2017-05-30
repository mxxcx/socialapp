import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { SocialappAvatarModule } from './avatar/avatar.module';
import { SocialappHeaderModule } from './header/header.module';
/* jhipster-needle-add-entity-module-import - JHipster will add entity modules imports here */

@NgModule({
    imports: [
        SocialappAvatarModule,
        SocialappHeaderModule,
        /* jhipster-needle-add-entity-module - JHipster will add entity modules here */
    ],
    declarations: [],
    entryComponents: [],
    providers: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class SocialappEntityModule {}
