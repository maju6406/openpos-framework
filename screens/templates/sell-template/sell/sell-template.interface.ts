import { IWorkStation } from './workstation.interface';
import { IMenuItem } from './../../../../core';

export interface ISellTemplate {
    scan: IScan;
    statusBar: IStatusBar;
    localMenuItems: IMenuItem[];
    transactionMenuItems: IMenuItem[];
    workstation: IWorkStation;
    operatorText: string;
    timestampBegin: number;
    registerStatus: string;
}

export interface IScan {
     scanMinLength: number;
     scanMaxLength: number;
     scanType: string;
     scanActionName: string;
     scanSomethingText: string;
     autoFocusOnScan: boolean;
}

export interface IStatusBar {
    backButton: IMenuItem;
    operatorText: String;
    logoutButton: IMenuItem;
    workstationId: String;
    showScan: boolean;
    showAdmin: boolean;
    showSkip: boolean;
}
