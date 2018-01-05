import {Parser} from 'xml2js';
import { ResponseBase } from './responsebase';

export class ShowScreenResponse extends ResponseBase {
    public static readonly requestType = 'ShowScreenRequest';

    protected makeResponseXml(requestJson: any): string {
        const req: any = requestJson[ShowScreenResponse.requestType];
        const xml =
`
<ShowScreenResponse>
    <POSID>${req.POSID}</POSID>
    <APPID>${req.APPID}</APPID>
    <CCTID>${req.CCTID}</CCTID>
    <ButtonReturn>01</ButtonReturn>
    <ResponseCode>00000</ResponseCode>
    <ResponseText>Approved</ResponseText>
</ShowScreenResponse>
`;
        return xml;
    }

}
