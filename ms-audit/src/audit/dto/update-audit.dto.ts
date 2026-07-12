import { PartialType } from '@nestjs/mapped-types';
import { CreateAuditEventDto } from './create-audit.dto';

export class UpdateAuditDto extends PartialType(CreateAuditEventDto) {}
