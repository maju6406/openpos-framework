import { AppVersion } from './../../common/appversion';
import { IMenuItem } from './../../common/imenuitem';
import { Component, OnInit } from '@angular/core';
import { IScreen } from '../../common';

declare var version: any; // read in from core at assets/version.js

@Component({
    selector: 'app-version',
    templateUrl: './version.component.html',
    styleUrls: ['./version.component.scss']
})
export class VersionComponent implements IScreen, OnInit {

    screen: any;
    versions: {id: string, name: string, version: string}[];
    primaryAction: IMenuItem;
    otherActions: IMenuItem[];

    constructor(public appVersion: AppVersion) {

    }

    ngOnInit(): void {
    }

    show(screen: any): void {
        this.screen = screen;
        this.versions = this.screen.versions;

        const clientBuildVersion = {id: 'clientBuildVersion', name: 'Client Build version',
            version: this.appVersion.buildVersion};
        this.versions.unshift(clientBuildVersion);

        this.appVersion.appVersion.then( v => {
            if (v !== 'n/a') {
                this.versions.unshift({id: 'clientAppVersion', name: 'Client App version',
                        version: v});
            }
        });

        if (screen.localMenuItems) {
            this.primaryAction = screen.localMenuItems[0];
        }

        if (screen.localMenuItems && screen.localMenuItems.length > 1) {
            this.otherActions = screen.localMenuItems.slice(1, screen.localMenuItems.length);
        }
    }
}
