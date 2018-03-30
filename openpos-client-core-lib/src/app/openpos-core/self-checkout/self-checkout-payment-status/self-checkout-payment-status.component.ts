import { Component, OnInit } from '@angular/core';

import { SessionService } from '../../services/session.service';
import { AbstractApp } from '../../common/abstract-app';

@Component({
  selector: 'app-self-checkout-payment-status-component',
  templateUrl: './self-checkout-payment-status.component.html'
})
export class SelfCheckoutPaymentStatusComponent implements OnInit {

  balanceDue: string = "0.00";
  instructions: string = "";
  additionalInstructions: string = "";

  constructor(public session: SessionService) { }

  show(screen: any, app: AbstractApp) {
  }

  ngOnInit() {
    this.balanceDue = this.session.screen.balanceDue;
    this.instructions = this.session.screen.instructions;
    if (this.session.screen.additionalInstructions) {
      this.additionalInstructions = this.session.screen.additionalInstructions;
    }
  }
}