# -*- coding: utf-8 -*-
# Generated by Django 1.9.6 on 2016-05-28 15:08
from __future__ import unicode_literals

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('summoners', '0004_auto_20160527_2047'),
    ]

    operations = [
        migrations.RenameField(
            model_name='summoner',
            old_name='last_updated',
            new_name='modified',
        ),
        migrations.AddField(
            model_name='user',
            name='created',
            field=models.DateTimeField(blank=True, editable=False, null=True),
        ),
    ]