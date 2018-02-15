import { Directive, Input, ElementRef, forwardRef, Renderer2, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { IFormatter } from './formatters/iformatter';
import { FormattersService } from './../services/formatters.service';

export const FORMATTED_INPUT_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => FormattedInputValueAccessor),
    multi: true
  };

@Directive({
    selector: 'input[InputFormatter]',
    host: { 
        '(onKeyPress)': 'onKeyPress($event)',
        '(input)': 'handleInput($event.target.value)'
     },
     providers: [FORMATTED_INPUT_VALUE_ACCESSOR]
})
export class FormattedInputValueAccessor implements ControlValueAccessor, OnInit {

    @Input() InputFormatter: string;

    private formatter: IFormatter;

    onChange = (value: string) => {};
    onTouched = () => {};

    constructor(private renderer: Renderer2, private elRef: ElementRef, private formatterService: FormattersService )  {
    }

    ngOnInit(): void {
        this.formatter = this.formatterService.getFormatter(this.InputFormatter);
    }

    writeValue(value: string): void {
        if( value ){
            this.renderer.setProperty( this.elRef.nativeElement, 'value', this.formatter.formatValue(value));
        }
    }

    registerOnChange(fn: (tel: string) => void ): void {
        this.onChange = fn;
    }

    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }

    handleInput(value: string){
        let cleanValue = this.formatter.unFormatValue(value);
        this.renderer.setProperty( this.elRef.nativeElement, 'value', this.formatter.formatValue(cleanValue));
        this.onChange(cleanValue);
    }

    onKeyPress(event: any){
        let inputChar = String.fromCharCode(event.charCode);

        if(!this.formatter.keyFilter.test(inputChar) && event.charCode != 13){
            event.preventDefault();
        }
    }
}
