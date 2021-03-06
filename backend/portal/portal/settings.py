import os
from portal.keys import DJANGO_SECRET_KEY
from portal.keys import EMAIL_PASSWORD
from portal.keys import RDS_PASSWORD

# django settings
ALLOWED_HOSTS = ['*']
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHES = {
    'default': {
        'BACKEND': 'django.core.cache.backends.memcached.MemcachedCache',
        'LOCATION': 'portal.vf9jgc.cfg.use1.cache.amazonaws.com:11211',
    }
}
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.mysql',
        'NAME': 'portaldb',
        'USER': 'tberroa',
        'PASSWORD': RDS_PASSWORD,
        'HOST': 'portal.cflq9mp1c8f1.us-east-1.rds.amazonaws.com',
        'PORT': '3306',
    }
}
DEBUG = False
INSTALLED_APPS = [
    'summoners',
    'stats',
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',
]
LOGGING = {
    'version': 1,
    'disable_existing_loggers': False,
    'formatters': {
        'standard': {
            'format': '%(asctime)s [%(levelname)s] %(name)s: %(message)s'
        },
    },
    'handlers': {
        'default': {
            'level': 'DEBUG',
            'class': 'logging.handlers.RotatingFileHandler',
            'filename': '/var/log/django.log',
            'maxBytes': 5242880,
            'formatter': 'standard',
        },
    },
    'loggers': {
        'django': {
            'handlers': ['default'],
            'level': 'DEBUG',
            'propagate': True
        },
    },
}
MIDDLEWARE_CLASSES = [
    'django.middleware.security.SecurityMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
]
PASSWORD_HASHERS = [
    'django.contrib.auth.hashers.MD5PasswordHasher',
]
ROOT_URLCONF = 'portal.urls'
TEMPLATES = [
    {
        'BACKEND': 'django.template.backends.django.DjangoTemplates',
        'DIRS': [],
        'APP_DIRS': True,
        'OPTIONS': {
            'context_processors': [
                'django.template.context_processors.debug',
                'django.template.context_processors.request',
                'django.contrib.auth.context_processors.auth',
                'django.contrib.messages.context_processors.messages',
            ],
        },
    },
]
TIME_ZONE = 'UTC'
USE_I18N = False
SECRET_KEY = DJANGO_SECRET_KEY
STATIC_ROOT = BASE_DIR + '/static/'
STATIC_URL = '/static/'
STATICFILES_FINDERS = [
    'django.contrib.staticfiles.finders.FileSystemFinder',
    'django.contrib.staticfiles.finders.AppDirectoriesFinder',
]
WSGI_APPLICATION = 'portal.wsgi.application'

# email settings
DEFAULT_FROM_EMAIL = 'quadrastats@outlook.com'
EMAIL_HOST = 'smtp-mail.outlook.com'
EMAIL_HOST_PASSWORD = EMAIL_PASSWORD
EMAIL_HOST_USER = 'quadrastats@outlook.com'
EMAIL_PORT = 587
EMAIL_USE_TLS = True
