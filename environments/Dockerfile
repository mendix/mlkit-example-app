# Use an official Python runtime as a parent image
FROM python:3.6-slim

# Set the working directory in the container
WORKDIR /workspace

# Install required packages and locales
RUN apt-get update && \
    apt-get install -y git locales && \
    echo "en_US.UTF-8 UTF-8" > /etc/locale.gen && \
    locale-gen en_US.UTF-8 && \
    update-locale LANG=en_US.UTF-8

ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

# Add build argument for requirements file
ARG REQUIREMENTS_FILE=reqirements-lts.txt

# Copy the requirements file based on the build argument
COPY ${REQUIREMENTS_FILE} requirements.txt

# Install the Python packages specified in requirements.txt
RUN pip install -r requirements.txt

# Install JupyterLab
RUN pip install jupyterlab

# Clone only the notebooks directory
RUN git init && \
    git remote add origin https://github.com/mendix/mlkit-example-app.git && \
    git config core.sparseCheckout true && \
    echo "notebooks/*" >> .git/info/sparse-checkout && \
    git pull origin main

# Copy the notebooks to the working directory
#RUN cp -r notebooks /workspace/notebooks

# Expose port 8888 for JupyterLab
EXPOSE 8888

# Start JupyterLab
CMD ["jupyter", "lab", "--ip=0.0.0.0", "--port=8888", "--no-browser", "--allow-root"]
